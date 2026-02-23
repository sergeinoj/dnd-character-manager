// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\FeatureEnricher.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.SelectionSource
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
    private val classFeatureRepository: ClassFeatureRepository
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_FEAT_ENRICHER"

    private data class BreathWeaponRule(val damageType: String, val shape: String, val saveStat: String)
    private val breathWeaponRules = mapOf(
        "acid" to BreathWeaponRule("Кислота", "5x30 ft. линия", "DEX"),
        "lightning" to BreathWeaponRule("Молния", "5x30 ft. линия", "DEX"),
        "fire" to BreathWeaponRule("Огонь", "15 ft. конус", "DEX"),
        "poison" to BreathWeaponRule("Яд", "15 ft. конус", "CON"),
        "cold" to BreathWeaponRule("Холод", "15 ft. конус", "CON")
    )

    suspend fun enrich(feature: Feature, entity: FeatureEntity, source: SelectionSource): Feature {
        return when (feature.index) {
            "draconic-ancestry", "dragon-ancestor" -> enrichDraconicAncestry(feature, entity, source)
            else -> feature
        }
    }

    private suspend fun enrichDraconicAncestry(feature: Feature, entity: FeatureEntity, source: SelectionSource): Feature {
        try {
            val choice = feature.choices.firstOrNull() as? FeatureChoiceDomain.SelectOption ?: return feature
            val choiceJson = entity.choicesJson?.let { rawJson -> runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() }

            val fromElement = choiceJson?.get("from")
            val optionsJson = when (fromElement) {
                is JsonArray -> fromElement
                is JsonObject -> fromElement["options"]?.jsonArray
                else -> null
            } ?: return feature

            val childFeatureIndexes = optionsJson.mapNotNull { it.jsonObject["value"]?.jsonPrimitive?.content }
            val childFeatures = if (childFeatureIndexes.isNotEmpty()) classFeatureRepository.getFeaturesByIndexes(childFeatureIndexes).associateBy { it.indexName } else emptyMap()

            val enrichedOptions = choice.options.mapNotNull { opt ->
                val childFeature = childFeatures[opt.id]
                val referenceJson = childFeature?.referenceJson?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }

                val (damageTypeIndex, breathType, breathSize) = if (referenceJson != null) {
                    Triple(
                        referenceJson["damage_type"]?.jsonPrimitive?.content,
                        referenceJson["breath"]?.jsonPrimitive?.content,
                        referenceJson["line_size"]?.jsonPrimitive?.content ?: referenceJson["cone_size"]?.jsonPrimitive?.intOrNull?.toString()
                    )
                } else {

                    val damageTypeFromId = opt.id.substringAfter("---", "").substringBefore("-damage")
                    val rule = breathWeaponRules[damageTypeFromId]
                    if (rule != null) {
                        Triple(damageTypeFromId, if (rule.shape.contains("линия")) "line" else "cone", if (rule.shape.contains("15")) "15" else "5x30")
                    } else {
                        Triple(null, null, null)
                    }
                }

                if (damageTypeIndex.isNullOrBlank()) {
                    Log.w(TAG, "Could not determine damage type for ancestry option: ${opt.id}")
                    return@mapNotNull opt
                }

                val damageTypeName = classFeatureRepository.getDamageTypeByIndex(damageTypeIndex)?.name ?: damageTypeIndex.capitalizeFirst()

                val (finalLabel, newInfoString) = when (source) {
                    SelectionSource.RACE -> {
                        val info = if (breathType != null && breathSize != null) {
                            val localizedBreathType = DndLocalization.translateBreathType(breathType)
                            val saveStatAbbr = DndLocalization.getBreathSaveStatAbbr(damageTypeIndex)
                            "$damageTypeName | $breathSize фт. $localizedBreathType ($saveStatAbbr)"
                        } else {
                            "Вид урона: $damageTypeName"
                        }
                        opt.label to info
                    }
                    SelectionSource.CLASS -> {
                        val info = "Родственная стихия: $damageTypeName"
                        val label = opt.label.substringBefore(" (").trim()
                        label to info
                    }
                    else -> {
                        val info = "Вид урона: $damageTypeName"
                        val label = opt.label.substringBefore(" (").trim()
                        label to info
                    }
                }

                opt.copy(label = finalLabel, info = newInfoString)
            }

            return feature.copy(choices = listOf(choice.copy(options = enrichedOptions)))
        } catch (e: Exception) {
            Log.e(TAG, "Error during enrichment for '${feature.index}'", e)
            return feature
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\FeatureEnricher.kt