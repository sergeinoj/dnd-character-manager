// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ValidationReport.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model


data class ValidationReport(
    val isValid: Boolean,
    val issues: List<ValidationIssue> = emptyList()
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ValidationReport.kt