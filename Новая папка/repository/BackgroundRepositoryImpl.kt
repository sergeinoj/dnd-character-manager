// Имя файла: app/src/main/java/com/dnd/app/data/repository/BackgroundRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.repository.datasource.BackgroundDataSource
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.usecase.BackgroundOrchestrator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundRepositoryImpl @Inject constructor(
    private val dataSource: BackgroundDataSource,
    private val referenceDao: ReferenceDao, // Оставляем для предзагрузки FeatureEntity
    private val orchestrator: BackgroundOrchestrator // [ОСНОВНАЯ ЗАВИСИМОСТЬ]
) : BackgroundRepository {

    private val TAG = "DND_DEBUG_BG_REPO"

    override suspend fun getBackgrounds(): List<Background> {
        return try {
            val entities = dataSource.loadAllBackgroundEntities()
            if (entities.isEmpty()) return emptyList()

            // ШАГ 1: Собираем все уникальные индексы способностей из всех предысторий
            val allFeatureIndexes = entities.flatMap { entity ->
                entity.featureIndicesJson?.let {
                    runCatching { kotlinx.serialization.json.Json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
                } ?: emptyList()
            }.distinct()

            // ШАГ 2: Один запрос к БД для получения всех нужных FeatureEntity
            val featureMap = if (allFeatureIndexes.isNotEmpty()) {
                referenceDao.getFeaturesByIndexes(allFeatureIndexes).associateBy { it.indexName }
            } else {
                emptyMap()
            }

            // ШАГ 3: Делегируем маппинг оркестратору для каждой сущности
            entities.mapNotNull { entity ->
                runCatching {
                    orchestrator.execute(entity, featureMap)
                }.onFailure { e ->
                    Log.e(TAG, "Orchestrator failed for background: ${entity.indexName}", e)
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in getBackgrounds", e)
            emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/BackgroundRepositoryImpl.kt