// Имя файла: app/src/main/java/com/dnd/app/domain/model/ParentRace.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * Структурированная раса для UI.
 * @param id ID родительской расы (например, Дварф)
 * @param name Имя родительской расы
 * @param subraces Список имен подрас, доступных для выбора (например, [Горный дварф, Холмовой дварф])
 */
data class ParentRace(
    val id: Int,
    val name: String,
    val subraces: List<String>
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ParentRace.kt