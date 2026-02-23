// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\model\FeatureJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProgressionSpellcastingJson(
    @SerialName("cantrips_known") val cantripsKnown: Int? = null,
    @SerialName("spells_known") val spellsKnown: Int? = null,
    @SerialName("spell_slots_level_1") val spellSlotsLevel1: Int? = null,
    @SerialName("pact_slots") val pactSlots: Int? = null,
    @SerialName("pact_slot_level") val pactSlotLevel: Int? = null,

    @SerialName("dynamic_preparation") val dynamicPreparation: Boolean = false
)


@Serializable
data class ClassSpecificJson(
    @SerialName("preparation_rule") val preparationRule: PreparationRuleJson? = null,
    @SerialName("resource_rules") val resourceRules: List<ResourceRuleJson> = emptyList()
)


@Serializable
data class PreparationRuleJson(
    val stat: String,
    val formula: String,
    @SerialName("min_limit") val minLimit: Int = 1
)

@Serializable
data class ResourceRuleJson(
    @SerialName("pool_name") val poolName: String? = null,
    val stat: String,
    val formula: String,
    @SerialName("min_limit") val minLimit: Int = 0,
    @SerialName("max_limit") val maxLimit: Int? = null
)


@Serializable
data class SubclassSpellsJson(
    val type: String? = null,
    val list: List<SubclassSpellLevelGroup> = emptyList()
)

@Serializable
data class SubclassSpellLevelGroup(
    val level: Int,
    val spells: List<String>
)

@Serializable
data class ChoiceJson(
    val choose: Int = 1,
    val type: String = "",
    val from: OptionSetJson? = null,
    val desc: String? = null
)

@Serializable
data class OptionSetJson(
    @SerialName("option_set_type") val optionSetType: String? = null,

    val options: List<OptionJson>? = null,
    @SerialName("equipment_category") val equipmentCategory: ReferenceJson? = null,
    val resource: String? = null
)

@Serializable
data class OptionJson(
    @SerialName("option_type") val optionType: String? = null,
    val item: ReferenceJson? = null,
    val choice: ChoiceJson? = null,
    val string: String? = null,
    val desc: String? = null,
    val value: String? = null,
    val label: String? = null
)

@Serializable
data class ReferenceJson(
    val index: String = "",
    val name: String = ""
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\model\FeatureJsonModels.kt
