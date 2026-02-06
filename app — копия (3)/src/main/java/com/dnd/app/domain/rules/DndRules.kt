// Имя файла: domain/rules/DndRules.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.rules

object DndRules {
    const val MAX_POINTS = 27
    const val MIN_SCORE = 8
    const val MAX_SCORE = 15
    val pointCost = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)
    fun getPointCost(score: Int): Int = pointCost[score] ?: 99
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/rules/DndRules.kt