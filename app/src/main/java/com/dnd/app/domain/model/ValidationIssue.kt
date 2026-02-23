// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ValidationIssue.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model


enum class SelectionSource {
    RACE, CLASS, BACKGROUND, INVENTORY
}


data class ValidationIssue(
    val source: SelectionSource,
    val featureName: String,
    val missingCount: Int,
    val tabIndex: Int
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ValidationIssue.kt