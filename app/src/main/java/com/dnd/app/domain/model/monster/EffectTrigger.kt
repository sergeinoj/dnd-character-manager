package com.dnd.app.domain.model.monster

import kotlinx.serialization.Serializable

@Serializable
data class EffectTrigger(
    val event: String,
    val condition: String?,
    val effectType: String,
    val target: String,
    val payloadSummary: String?,
    val saveDc: Int?,
    val saveStat: String?
) {
    fun displayLabel(): String {
        val eventLabel = event
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        val conditionPart = condition?.let { " when $it" } ?: ""
        val payloadPart = payloadSummary?.let { ": $it" } ?: ""
        val savePart = saveStat?.let { " (DC ${saveDc ?: 10} ${it.uppercase()})" } ?: ""
        return "$eventLabel$conditionPart -> $effectType$payloadPart$savePart"
    }
}
