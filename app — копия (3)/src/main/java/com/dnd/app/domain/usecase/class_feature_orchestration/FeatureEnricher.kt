// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/FeatureEnricher.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureEnricher @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository // ИЗМЕНЕНО
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_FEAT_ENRICHER"

    suspend fun enrich(feature: Feature, entity: FeatureEntity): Feature {
        return when (feature.index) {
            "draconic-ancestry", "dragon-ancestor" -> enrichDraconicAncestry(feature, entity)
            else -> feature
        }
    }

    private suspend fun enrichDraconicAncestry(feature: Feature, entity: FeatureEntity): Feature {
        try {
            val choice = feature.choices.firstOrNull() as? FeatureChoiceDomain.SelectOption ?: return feature
            val choiceJson = entity.choicesJson?.let { rawJson -> json.parseToJsonElement(rawJson).jsonObject } ?: return feature

            val fromElement = choiceJson["from"]
            val optionsJson = when (fromElement) {
                is JsonArray -> fromElement
                is JsonObject -> fromElement["options"]?.jsonArray
                else -> null
            } ?: return feature

            val childFeatureIndexes = optionsJson.mapNotNull { it.jsonObject["value"]?.jsonPrimitive?.content }
            if (childFeatureIndexes.isEmpty()) return feature

            val childFeatures = classFeatureRepository.getFeaturesByIndexes(childFeatureIndexes).associateBy { it.indexName }
            if (childFeatures.isEmpty()) return feature

            val enrichedOptions = choice.options.mapNotNull { opt ->
                val childFeature = childFeatures[opt.id]
                val referenceJson = childFeature?.referenceJson?.let { json.parseToJsonElement(it).jsonObject }

                if (referenceJson == null) {
                    Log.w(TAG, "No reference_json found for child feature: ${opt.id}")
                    return@mapNotNull opt
                }

                val damageTypeIndex = referenceJson["damage_type"]?.jsonPrimitive?.content ?: ""
                val damageTypeName = classFeatureRepository.getDamageTypeByIndex(damageTypeIndex)?.name ?: damageTypeIndex.capitalizeFirst()

                val newInfoString = "Вид урона: $damageTypeName"
                val newLabel = opt.label.substringBefore(" (").trim()

                opt.copy(label = newLabel, info = newInfoString)
            }

            return feature.copy(choices = listOf(choice.copy(options = enrichedOptions)))
        } catch (e: Exception) {
            Log.e(TAG, "Error during enrichment for '${feature.index}'", e)
            return feature
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/FeatureEnricher.kt