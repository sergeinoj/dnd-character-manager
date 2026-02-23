package com.dnd.app.domain.model.magic

import kotlinx.serialization.Serializable

@Serializable
data class SpellCastContext(
    val id: String,
    val name: String,
    val isConcentration: Boolean
)
