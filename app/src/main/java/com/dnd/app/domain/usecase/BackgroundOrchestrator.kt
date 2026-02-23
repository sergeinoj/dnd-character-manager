// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\BackgroundOrchestrator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.BackgroundEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureFactory
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundOrchestrator @Inject constructor(
    private val referenceDao: ReferenceDao,
    private val featureFactory: FeatureFactory,
    private val json: Json
) {
    private val TAG = "DND_LOG_BG_ORCH"


    suspend fun execute(
        entity: BackgroundEntity,
        allFeaturesMap: Map<String, FeatureEntity>
    ): Background {
        val allFeatures = mutableListOf<Feature>()
        var extraGoldFromEquipment = 0


        if (!entity.featureName.isNullOrBlank()) {
            allFeatures.add(
                Feature(
                    id = -1,
                    index = "legacy-${entity.indexName}",
                    name = entity.featureName,
                    description = entity.featureDesc?.stripHtml() ?: "",
                    uiGroup = "BACKGROUND",
                    priority = 1
                )
            )
        }


        val indexedIds = entity.featureIndicesJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
        } ?: emptyList()

        for (index in indexedIds) {
            val featEntity = allFeaturesMap[index] ?: referenceDao.getFeatureByIndex(index)
            if (featEntity != null) {
                allFeatures.add(featureFactory.create(featEntity))
            } else {
                Log.w(TAG, "Indexed feature not found: $index")
            }
        }


        val contextEntities = referenceDao.findFeaturesByContext(bgIdx = entity.indexName)
        for (featEntity in contextEntities) {
            if (allFeatures.none { it.index == featEntity.indexName }) {
                allFeatures.add(featureFactory.create(featEntity))
            }
        }


        val filteredEquipment = mutableListOf<String>()
        entity.startingEquipmentJson?.let { raw ->
            runCatching {
                val element = json.parseToJsonElement(raw)
                if (element is JsonArray) {
                    element.forEach { item ->
                        if (item is JsonObject) {
                            val idx = item["index"]?.jsonPrimitive?.content
                            if (idx == "gold") {
                                extraGoldFromEquipment += item["quantity"]?.jsonPrimitive?.intOrNull ?: 0
                            } else if (idx != null) {
                                filteredEquipment.add(idx)
                            }
                        } else if (item is JsonPrimitive) {
                            filteredEquipment.add(item.content)
                        }
                    }
                }
            }.onFailure { Log.e(TAG, "Failed to parse equipment for gold extraction: ${entity.indexName}") }
        }


        val processedFeatures = allFeatures.map { f ->
            val newPriority = when {
                f.index.endsWith("-lore") -> 1
                f.index.contains("legacy-") -> 2
                f.index.endsWith("-skills") -> 10
                f.choices.isNotEmpty() -> 20
                else -> 100
            }
            f.copy(priority = newPriority)
        }


        val staticProficienciesFromEntity = parseSimpleRefList(entity.startingProficienciesJson)
        val profsFromFeatures = processedFeatures.flatMap { it.grantedProficiencies }
        val finalStaticProficiencies = (staticProficienciesFromEntity + profsFromFeatures).distinct()

        return Background(
            id = entity.id ?: 0,
            index = entity.indexName,
            name = entity.name,
            features = processedFeatures.distinctBy { it.index }.sortedBy { it.priority },
            staticProficiencies = finalStaticProficiencies,
            equipment = filteredEquipment.distinct(),
            startingGold = entity.startingGold + extraGoldFromEquipment,
            featureIndices = indexedIds,
            personalityTraits = parseStringList(entity.personalityTraitsJson),
            ideals = parseStringList(entity.idealsJson),
            bonds = parseStringList(entity.bondsJson),
            flaws = parseStringList(entity.flawsJson)
        )
    }

    private fun parseSimpleRefList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val element = json.parseToJsonElement(raw)
            if (element is JsonArray) {
                element.mapNotNull { item ->
                    when (item) {
                        is JsonObject -> item["index"]?.jsonPrimitive?.content
                        is JsonPrimitive -> item.content
                        else -> null
                    }
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrElse { emptyList() }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\BackgroundOrchestrator.kt