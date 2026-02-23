package com.dnd.app.domain.model.condition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConditionDefinition(
    val indexName: String,
    val name: String,
    val description: String?,
    val uiColorHex: String?,
    val mechanics: ConditionMechanics
)

@Serializable
data class ConditionMechanics(
    @SerialName("attack_disadvantage") val attackDisadvantage: Boolean = false,
    @SerialName("attack_advantage_against") val attackAdvantageAgainst: Boolean = false,
    @SerialName("incapacitated") val incapacitated: Boolean = false,
    @SerialName("speed_multiplier") val speedMultiplier: Double = 1.0,
    @SerialName("speed_override") val speedOverride: Double? = null,
    @SerialName("hp_max_multiplier") val hpMaxMultiplier: Double? = null,
    @SerialName("fail_save") val failSave: List<String> = emptyList(),
    @SerialName("check_disadvantage") val checkDisadvantage: List<String> = emptyList(),
    @SerialName("save_disadvantage") val saveDisadvantage: List<String> = emptyList(),
    @SerialName("auto_crit_within_5ft") val autoCritWithin5Ft: Boolean = false,
    @SerialName("rest_reduction") val restReduction: Double? = null,
    @SerialName("is_dead") val isDead: Boolean = false
)
