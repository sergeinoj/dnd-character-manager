// Имя файла: domain/calculator/DndCalculator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.calculator

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

@Singleton
class DndCalculator @Inject constructor() {

    fun calculateModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }

    fun calculateProficiencyBonus(level: Int): Int {
        return (level - 1) / 4 + 2
    }

    fun formatModifier(mod: Int): String {
        return if (mod >= 0) "+$mod" else "$mod"
    }

    fun calculateBaseAC(dexModifier: Int): Int {
        return 10 + dexModifier
    }

    // Новый метод: Расчет бонуса навыка
    fun calculateSkillBonus(score: Int, profBonus: Int, multiplier: Int): Int {
        val mod = calculateModifier(score)
        return mod + (profBonus * multiplier)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/calculator/DndCalculator.kt