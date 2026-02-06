// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/FeatureFactory.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.WeaponEntity
import com.dnd.app.data.repository.datasource.SpellDataSource
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFactory @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val spellDataSource: SpellDataSource
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_FEAT_FACTORY"

    suspend fun create(entity: FeatureEntity): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        val isExpertiseFeature = entity.indexName.contains("expertise")

        entity.choicesJson?.let { raw ->
            try {
                val el = json.parseToJsonElement(raw)
                val elements = if (el is JsonArray) el else listOf(el)
                elements.forEach {
                    if (it is JsonObject) choices.add(parseChoice(it, isExpertiseFeature))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing choices_json for feature ${entity.indexName}", e)
            }
        }

        entity.referenceJson?.let { raw ->
            try {
                val refObj = json.parseToJsonElement(raw).jsonObject
                refObj["specific"]?.jsonObject?.let { specific ->
                    specific.entries.forEach { (key, value) ->
                        if (key.endsWith("_options") && value is JsonObject) {
                            choices.add(parseChoice(value, isExpertiseFeature))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing reference_json for feature ${entity.indexName}", e)
            }
        }

        val spells = spellDataSource.getGrantedSpells(entity.spellShowJson)
        val grantedProficiencies = mutableListOf<String>()
        entity.referenceJson?.let { raw ->
            try {
                val refObj = json.parseToJsonElement(raw).jsonObject
                refObj["granted_proficiencies"]?.jsonArray?.forEach { prof ->
                    prof.jsonObject["index"]?.jsonPrimitive?.content?.let { grantedProficiencies.add(it) }
                }
            } catch (e: Exception) { /* Malformed JSON */ }
        }

        return Feature(
            id = entity.id ?: 0,
            index = entity.indexName,
            name = entity.name,
            subclassIndex = entity.subclassIndex,
            description = entity.description?.stripHtml() ?: "",
            choices = choices,
            embeddedSpells = spells,
            changeRule = entity.changeRule == 1,
            priority = 100,
            grantedProficiencies = grantedProficiencies,
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup
        )
    }

    suspend fun parseChoice(obj: JsonObject, isExpertiseContext: Boolean = false): FeatureChoiceDomain {
        val type = obj["type"]?.jsonPrimitive?.content ?: ""
        val effectiveExpertiseContext = isExpertiseContext || type.contains("expertise")
        Log.d(TAG, "--- Parsing Choice (isExpertise=$effectiveExpertiseContext) --- \nJSON: ${obj.toString().take(200)}...")
        val count = obj["choose"]?.jsonPrimitive?.int ?: 1

        if (effectiveExpertiseContext && (type == "proficiency" || type == "proficiencies" || type == "skill")) {
            val fromElement = obj["from"]
            if (fromElement is JsonObject && (fromElement["option_set_type"]?.jsonPrimitive?.content == "resource_list" || fromElement["option_set_type"]?.jsonPrimitive?.content == "options_array")) {
                Log.d(TAG, "[Expertise Context] Detected skill choice. Returning empty options for UI white-listing.")
                return FeatureChoiceDomain.SelectExpertise(count, emptyList())
            }
        }

        if (type == "ability-score-improvement") {
            Log.d(TAG, "Detected 'ability-score-improvement'. Creating complex ASI/Feat choice.")
            val statsOptions = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA").map { ChoiceOption(it, DndLocalization.translateStat(it)) }
            val allFeats = classFeatureRepository.getAllFeats().map { ChoiceOption(it.indexName, it.name) }
            val options = listOf(
                ChoiceOption(id = "asi", label = "Увеличение характеристик (+2)", subChoice = FeatureChoiceDomain.SelectStatBonus(2, 1, statsOptions)),
                ChoiceOption(id = "feat", label = "Черта", subChoice = FeatureChoiceDomain.SelectOption(1, allFeats))
            )
            return FeatureChoiceDomain.SelectOption(1, options, "Выберите улучшение")
        }

        if (type.contains("spell")) {
            Log.d(TAG, "Detected spell choice. Delegating to SpellDataSource.")
            return spellDataSource.parseSpellChoice(obj)
        }

        val options = mutableListOf<ChoiceOption>()
        val fromElement = obj["from"] ?: return FeatureChoiceDomain.SelectOption(0, emptyList())

        Log.d(TAG, "Processing 'from' element of type: ${fromElement::class.simpleName}")

        val optionSetType = if (fromElement is JsonObject) fromElement["option_set_type"]?.jsonPrimitive?.content else null
        Log.d(TAG, "Detected option_set_type: $optionSetType")

        when (optionSetType) {
            "equipment_category" -> {
                Log.d(TAG, "[Case: equipment_category]")
                val categoryIndex = fromElement.jsonObject["equipment_category"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                if (categoryIndex != null) {
                    val itemIndexes = classFeatureRepository.getLinksForCategory(categoryIndex)
                    if (itemIndexes.isNotEmpty()) {
                        val equipment = classFeatureRepository.getEquipmentByIndexes(itemIndexes)
                        val weapons = classFeatureRepository.getWeaponsByIndexes(itemIndexes)
                        val armors = classFeatureRepository.getArmorByIndexes(itemIndexes)
                        equipment.mapTo(options) { mapEquipmentToChoiceOption(it) }
                        weapons.mapTo(options) { mapWeaponToChoiceOption(it) }
                        armors.mapTo(options) { mapArmorToChoiceOption(it) }
                    } else {
                        // Fallback для категорий, определенных напрямую в таблице equipment
                        val simpleEquipment = classFeatureRepository.getEquipmentByCategory(categoryIndex)
                        simpleEquipment.mapTo(options) { mapEquipmentToChoiceOption(it) }
                    }
                }
            }
            "resource_list" -> {
                Log.d(TAG, "[Case: resource_list]")
                when (val resource = fromElement.jsonObject["resource"]?.jsonPrimitive?.content) {
                    "skills" -> classFeatureRepository.getAllSkills().forEach { options.add(ChoiceOption("skill-${it.indexName}", it.name)) }
                    "languages" -> classFeatureRepository.getAllLanguages().forEach { options.add(ChoiceOption(it.indexName, it.name)) }
                    else -> Log.w(TAG, "Unknown resource_list type: $resource")
                }
            }
            "options_array" -> {
                Log.d(TAG, "[Case: options_array]")
                fromElement.jsonObject["options"]?.jsonArray?.forEach { el ->
                    if (el is JsonObject) {
                        val optionType = el["option_type"]?.jsonPrimitive?.content
                        var handled = false
                        if (optionType == "choice") {
                            el["choice"]?.jsonObject?.let { nestedChoiceObj ->
                                val subChoiceDomain = parseChoice(nestedChoiceObj, effectiveExpertiseContext)
                                val categoryIndex = nestedChoiceObj["from"]?.jsonObject?.get("equipment_category")?.jsonObject?.get("index")?.jsonPrimitive?.content
                                val id = "nested-group-${categoryIndex ?: subChoiceDomain.hashCode()}"
                                val label = el["desc"]?.jsonPrimitive?.content
                                    ?: nestedChoiceObj["desc"]?.jsonPrimitive?.content
                                    ?: (categoryIndex?.let { classFeatureRepository.getEquipmentCategoryByIndex(it)?.name } ?: "Выбор навыков")
                                options.add(ChoiceOption(id, label, subChoice = subChoiceDomain))
                            }
                            handled = true
                        } else if (optionType == "multiple") {
                            val items = el["items"]?.jsonArray ?: JsonArray(emptyList())
                            val subChoices = mutableListOf<ChoiceOption>()
                            items.forEach { item ->
                                val itemObj = item.jsonObject
                                when (itemObj["option_type"]?.jsonPrimitive?.content) {
                                    "choice" -> itemObj["choice"]?.jsonObject?.let {
                                        subChoices.add(ChoiceOption("dynamic-choice", "Выберите навык", subChoice = parseChoice(it, effectiveExpertiseContext)))
                                    }
                                    "reference" -> itemObj["item"]?.jsonObject?.let {
                                        val refId = "tool-${it["index"]?.jsonPrimitive?.content}"
                                        val refName = it["name"]?.jsonPrimitive?.content ?: "Инструменты"
                                        subChoices.add(ChoiceOption(refId, refName, info = "Автоматически"))
                                    }
                                }
                            }
                            options.add(ChoiceOption("multiple-bundle-${el.hashCode()}", "Навык и Воровские инструменты", subChoice = FeatureChoiceDomain.SelectOption(1, subChoices, "@CONTAINER@")))
                            handled = true
                        }
                        if (!handled) {
                            try {
                                val item = el["item"]?.jsonObject
                                val rawId = item?.get("index")?.jsonPrimitive?.content ?: el["value"]?.jsonPrimitive?.content ?: ""
                                if (rawId.isNotBlank()) {
                                    val label = item?.get("name")?.jsonPrimitive?.content ?: el["label"]?.jsonPrimitive?.content ?: DndLocalization.translateSkill(rawId)
                                    val info = el["desc"]?.jsonPrimitive?.content
                                    val subChoice = el["choice"]?.jsonObject?.let { parseChoice(it, effectiveExpertiseContext) }
                                    options.add(ChoiceOption(rawId, DndLocalization.cleanLabel(label), info, subChoice))
                                }
                            } catch (e: Exception) { Log.e(TAG, "Structural parser in options_array failed", e) }
                        }
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unhandled or null optionSetType. Falling back to legacy parsing.")
                val optionsSource = when (fromElement) {
                    is JsonObject -> fromElement["options"]
                    is JsonArray -> fromElement
                    else -> null
                }
                if (optionsSource is JsonArray) {
                    optionsSource.forEach { el ->
                        try {
                            when (el) {
                                is JsonObject -> {
                                    val item = el["item"]?.jsonObject
                                    val rawId = item?.get("index")?.jsonPrimitive?.content ?: el["value"]?.jsonPrimitive?.content ?: ""
                                    val id = if (rawId.startsWith("skill-", ignoreCase = true)) rawId else rawId
                                    val label = item?.get("name")?.jsonPrimitive?.content ?: el["label"]?.jsonPrimitive?.content ?: DndLocalization.translateSkill(id)
                                    val info = el["desc"]?.jsonPrimitive?.content
                                    val subChoice = el["choice"]?.jsonObject?.let { parseChoice(it, effectiveExpertiseContext) }
                                    options.add(ChoiceOption(id, DndLocalization.cleanLabel(label), info, subChoice))
                                }
                                else -> {
                                    val id = el.jsonPrimitive.content
                                    options.add(ChoiceOption(id, DndLocalization.translateSkill(id)))
                                }
                            }
                        } catch (e: Exception) { Log.e(TAG, "Legacy parser failed on element: $el", e) }
                    }
                }
            }
        }

        val result = when {
            effectiveExpertiseContext -> FeatureChoiceDomain.SelectExpertise(count, options)
            type.contains("ability") -> FeatureChoiceDomain.SelectStatBonus(count, 1, options)
            type.contains("skill") || type.contains("proficiencies") || type.contains("proficiency") -> FeatureChoiceDomain.SelectSkill(count, options)
            type.contains("feature") || type.contains("string") || type.contains("dragon_color") -> FeatureChoiceDomain.SelectOption(count, options)
            else -> FeatureChoiceDomain.SelectOption(count, options, DndLocalization.translateFeatureChoiceHeader(type))
        }
        Log.d(TAG, "--- Finished Parsing Choice --- \nResult: $result")
        return result
    }

    private fun parseCost(costJson: String?): String {
        if (costJson.isNullOrBlank()) return "0"
        return try {
            val obj = json.parseToJsonElement(costJson).jsonObject
            val quantity = obj["quantity"]?.jsonPrimitive?.int ?: 0
            val unit = obj["unit"]?.jsonPrimitive?.content ?: ""
            "$quantity $unit"
        } catch (e: Exception) { "N/A" }
    }

    private fun formatInfo(cost: String, weight: Double?): String {
        val weightStr = weight?.let { " | Вес: $it фнт." } ?: ""
        return "[Цена: $cost$weightStr]"
    }

    private fun mapEquipmentToChoiceOption(entity: EquipmentEntity): ChoiceOption {
        val cost = parseCost(entity.costJson)
        return ChoiceOption(
            id = entity.indexName,
            label = entity.name,
            info = formatInfo(cost, entity.weight)
        )
    }

    private fun mapWeaponToChoiceOption(entity: WeaponEntity): ChoiceOption {
        return ChoiceOption(
            id = entity.indexName,
            label = entity.name,
            info = formatInfo(entity.cost ?: "0", entity.weight)
        )
    }

    private fun mapArmorToChoiceOption(entity: ArmorEntity): ChoiceOption {
        return ChoiceOption(
            id = entity.indexName,
            label = entity.name,
            info = formatInfo(entity.cost ?: "0", entity.weight)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/FeatureFactory.kt