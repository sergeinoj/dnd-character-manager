package com.dnd.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LssBackupContainer(
    val tags: List<String> = emptyList(),
    val disabledBlocks: JsonObject? = null,
    val edition: String? = null,
    val data: String,
    val spells: LssSpellSummary = LssSpellSummary(),
    val jsonType: String = "character",
    val version: String = "2"
)

@Serializable
data class LssSpellSummary(
    val mode: String = "cards",
    val prepared: List<String> = emptyList(),
    val book: List<String> = emptyList(),
    val edition: String? = null
)
