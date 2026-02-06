// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/DraftStatsUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централизованный калькулятор для агрегации всех бонусов характеристик в черновике.
 * Является единым источником истины для поля `aggregateStatBonuses` в DraftCharacter.
 */
@Singleton
class DraftStatsUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(draft: DraftCharacter): DraftCharacter {
        val totalBonuses = mutableMapOf<String, Int>()

        // Шаг 1: Расчет Родительской Расы
        if (draft.baseInfo.raceIndex.isNotBlank()) {
            val race = repository.getRaceByIndex(draft.baseInfo.raceIndex)
            race?.baseStats?.forEach { (stat, bonus) ->
                totalBonuses[stat] = (totalBonuses[stat] ?: 0) + bonus
            }
        }

        // Шаг 2: Расчет Подрасы
        if (!draft.baseInfo.subraceIndex.isNullOrBlank()) {
            val subrace = repository.getSubraceModelByIndex(draft.baseInfo.subraceIndex)
            subrace?.baseStats?.forEach { (stat, bonus) ->
                totalBonuses[stat] = (totalBonuses[stat] ?: 0) + bonus
            }
        }

        // Шаг 3: Расчет Черт и Выборов
        val allSelections = draft.baseInfo.raceSelections.values +
                draft.baseInfo.backgroundSelections.values +
                draft.levelStack.flatMap { it.selections.values }

        allSelections.filterIsInstance<ChoiceResult.StatBonus>().forEach { result ->
            result.bonuses.forEach { (stat, bonus) ->
                val upperStat = stat.take(3).uppercase()
                totalBonuses[upperStat] = (totalBonuses[upperStat] ?: 0) + bonus
            }
        }

        // Шаг 4: Финализация
        return draft.copy(
            baseInfo = draft.baseInfo.copy(
                aggregateStatBonuses = totalBonuses
            )
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/DraftStatsUseCase.kt