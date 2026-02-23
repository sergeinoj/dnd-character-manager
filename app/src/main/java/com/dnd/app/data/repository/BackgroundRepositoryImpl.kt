// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\BackgroundRepositoryImpl.kt
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
    private val referenceDao: ReferenceDao,
    private val orchestrator: BackgroundOrchestrator
) : BackgroundRepository {

    private val TAG = "DND_DEBUG_BG_REPO"

    override suspend fun getBackgrounds(): List<Background> {
        return try {
            val entities = dataSource.loadAllBackgroundEntities()
            if (entities.isEmpty()) return emptyList()


            val allFeatureIndexes = entities.flatMap { entity ->
                entity.featureIndicesJson?.let {
                    runCatching { kotlinx.serialization.json.Json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
                } ?: emptyList()
            }.distinct()


            val featureMap = if (allFeatureIndexes.isNotEmpty()) {
                referenceDao.getFeaturesByIndexes(allFeatureIndexes).associateBy { it.indexName }
            } else {
                emptyMap()
            }


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

    override suspend fun getBackgroundByIndex(index: String): Background? {
        return try {
            val entity = dataSource.loadBackgroundEntityByIndex(index) ?: return null

            val featureIndexes = entity.featureIndicesJson?.let {
                runCatching { kotlinx.serialization.json.Json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
            } ?: emptyList()

            val featureMap = if (featureIndexes.isNotEmpty()) {
                referenceDao.getFeaturesByIndexes(featureIndexes).associateBy { it.indexName }
            } else {
                emptyMap()
            }

            runCatching {
                orchestrator.execute(entity, featureMap)
            }.onFailure { e ->
                Log.e(TAG, "Orchestrator failed for single background: ${entity.indexName}", e)
            }.getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in getBackgroundByIndex for index: $index", e)
            null
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\BackgroundRepositoryImpl.kt