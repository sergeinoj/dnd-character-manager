// Имя файла: app/src/main/java/com/dnd/app/data/repository/BackgroundRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.repository.datasource.BackgroundDataSource
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureFactory
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class BackgroundRepositoryImpl @Inject constructor(
    private val dataSource: BackgroundDataSource,
    private val referenceDao: ReferenceDao,
    private val featureFactoryProvider: Provider<FeatureFactory>
) : BackgroundRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_BG_REPO"

    override suspend fun getBackgrounds(): List<Background> {
        return try {
            val entities = dataSource.loadAllBackgroundEntities()
            if (entities.isEmpty()) return emptyList()

            val featureFactory = featureFactoryProvider.get()

            // ШАГ 1: Index Resolution - собираем все уникальные индексы фич одним махом
            val allFeatureIndexes = entities.flatMap { entity ->
                entity.featureIndicesJson?.let {
                    try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
                } ?: emptyList()
            }.distinct()

            // ШАГ 2: Один запрос к БД для получения всех нужных FeatureEntity
            val featureMap = if (allFeatureIndexes.isNotEmpty()) {
                referenceDao.getFeaturesByIndexes(allFeatureIndexes).associateBy { it.indexName }
            } else {
                emptyMap()
            }

            // ШАГ 3: Маппинг с обработкой ошибок для каждой предыстории (Песочница)
            entities.mapNotNull { entity ->
                runCatching {
                    mapEntityToDomain(entity, featureMap, featureFactory)
                }.onFailure { e ->
                    Log.e(TAG, "Failed to parse background: ${entity.indexName}", e)
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in getBackgrounds", e)
            emptyList()
        }
    }

    private suspend fun mapEntityToDomain(
        entity: com.dnd.app.data.local.entity.BackgroundEntity,
        featureMap: Map<String, FeatureEntity>,
        factory: FeatureFactory
    ): Background {
        val allFeatures = mutableListOf<Feature>()

        // Разрешаем индексы способностей
        val featureIndices = entity.featureIndicesJson?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()

        featureIndices.forEach { index ->
            featureMap[index]?.let { featEntity ->
                allFeatures.add(factory.create(featEntity))
            }
        }

        // Если индексов нет, пробуем старый способ (fallback на выборы из JSON)
        if (featureIndices.isEmpty()) {
            parseLegacyChoices(entity, factory, allFeatures)
        }
        
        // Обработка выбора снаряжения (v1.26)
        entity.startingEquipmentOptionsJson?.let { raw ->
            try {
                json.decodeFromString<List<JsonObject>>(raw).forEachIndexed { i, choiceJson ->
                    val choiceDomain = factory.parseChoice(choiceJson)
                    val name = (choiceDomain as? FeatureChoiceDomain.SelectOption)?.description ?: "Снаряжение предыстории"
                    allFeatures.add(Feature(
                        id = -500 - (entity.id ?: 0) - i,
                        index = "bg-equip-choice-${entity.indexName}-$i",
                        name = name,
                        description = "Выберите стартовое снаряжение.",
                        choices = listOf(choiceDomain),
                        uiGroup = "INVENTORY"
                    ))
                }
            } catch (e: Exception) { /* Log skip */ }
        }

        return Background(
            id = entity.id ?: 0,
            name = entity.name,
            features = allFeatures.distinctBy { it.index },
            staticEquipment = parseSimpleReference(entity.startingEquipmentJson),
            startingGold = entity.startingGold ?: 0,
            featureIndices = featureIndices,
            personalityTraits = parseJsonStrings(entity.personalityTraitsJson),
            ideals = parseJsonStrings(entity.idealsJson),
            bonds = parseJsonStrings(entity.bondsJson),
            flaws = parseJsonStrings(entity.flawsJson)
        )
    }

    private suspend fun parseLegacyChoices(entity: com.dnd.app.data.local.entity.BackgroundEntity, factory: FeatureFactory, out: MutableList<Feature>) {
        entity.startingProficienciesJson?.let { raw ->
            try {
                val obj = json.parseToJsonElement(raw).jsonObject
                val choice = factory.parseChoice(obj)
                out.add(Feature(
                    id = -300 - (entity.id ?: 0),
                    index = "bg-prof-choice-${entity.indexName}",
                    name = "Владения предыстории",
                    description = "Выберите навыки, предоставляемые вашей предысторией.",
                    choices = listOf(choice),
                    uiGroup = "SKILLS"
                ))
            } catch (e: Exception) { }
        }
    }

    private fun parseSimpleReference(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val element = json.parseToJsonElement(raw)
            when (element) {
                is JsonArray -> element.mapNotNull { it.jsonObject["index"]?.jsonPrimitive?.content }
                is JsonObject -> listOfNotNull(element["index"]?.jsonPrimitive?.content)
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsonStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try { json.decodeFromString<List<String>>(raw) } catch (e: Exception) { emptyList() }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/BackgroundRepositoryImpl.kt