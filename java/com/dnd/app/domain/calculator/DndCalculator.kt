// Имя файла: app/src/main/java/com/dnd/app/domain/calculator/DndCalculator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.calculator

import com.dnd.app.domain.model.DraftCharacter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

@Singleton
class DndCalculator @Inject constructor() {

    fun calculateModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }

    fun calculateProficiencyBonus(totalLevel: Int): Int {
        return when {
            totalLevel >= 17 -> 6
            totalLevel >= 13 -> 5
            totalLevel >= 9 -> 4
            totalLevel >= 5 -> 3
            else -> 2
        }
    }

    fun formatModifier(mod: Int): String {
        return if (mod >= 0) "+$mod" else "$mod"
    }

    fun calculateSkillBonus(score: Int, profBonus: Int, multiplier: Int): Int {
        val mod = calculateModifier(score)
        return mod + (profBonus * multiplier)
    }

    // ВОССТАНОВЛЕННЫЙ МЕТОД
    fun calculateBaseAC(dexModifier: Int): Int {
        // Базовая логика 10 + ловкость.
        // Для брони нужно будет расширять логику, но пока так.
        return 10 + dexModifier
    }

    /**
     * [НОВЫЙ МЕТОД - ЭТАП 7]
     * Определяет основную характеристику для заклинаний для указанного класса.
     */
    fun getPrimaryCastingStat(classIndex: String): String? {
        return when (classIndex) {
            "cleric", "druid", "ranger" -> "WIS"
            "wizard" -> "INT"
            "paladin", "bard", "sorcerer", "warlock" -> "CHA"
            else -> null
        }
    }

    /**
     * [НОВЫЙ МЕТОД - ЭТАП 7]
     * Вычисляет модификатор основной характеристики заклинателя на основе черновика.
     */
    fun calculateRelevantAbilityModifier(draft: DraftCharacter): Int {
        val classIndex = draft.levelStack.firstOrNull()?.classIndex ?: return 0
        val primaryStat = getPrimaryCastingStat(classIndex) ?: return 0
        val baseScore = draft.baseInfo.baseAbilityScores[primaryStat] ?: 10
        val bonus = draft.baseInfo.aggregateStatBonuses[primaryStat] ?: 0
        val totalScore = baseScore + bonus
        return calculateModifier(totalScore)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/calculator/DndCalculator.kt