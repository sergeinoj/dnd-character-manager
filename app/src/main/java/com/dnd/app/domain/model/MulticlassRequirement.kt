// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/domain/model/MulticlassRequirement.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MulticlassRequirement(
    val stat: String,
    val minScore: Int
)
// --- КОНЕЦ ФАЙЛА ---
