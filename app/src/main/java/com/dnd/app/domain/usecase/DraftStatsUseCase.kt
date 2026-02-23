// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\DraftStatsUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DraftStatsUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(draft: DraftCharacter): DraftCharacter {
        val totalBonuses = mutableMapOf<String, Int>()


        if (draft.baseInfo.raceIndex.isNotBlank()) {
            val race = repository.getRaceByIndex(draft.baseInfo.raceIndex)
            race?.baseStats?.forEach { (stat, bonus) ->
                totalBonuses[stat] = (totalBonuses[stat] ?: 0) + bonus
            }
        }


        if (!draft.baseInfo.subraceIndex.isNullOrBlank()) {
            val subrace = repository.getSubraceModelByIndex(draft.baseInfo.subraceIndex)
            subrace?.baseStats?.forEach { (stat, bonus) ->
                totalBonuses[stat] = (totalBonuses[stat] ?: 0) + bonus
            }
        }


        val allSelections = draft.baseInfo.raceSelections.values +
                draft.baseInfo.backgroundSelections.values +
                draft.levelStack.flatMap { it.selections.values }

        allSelections.filterIsInstance<ChoiceResult.StatBonus>().forEach { result ->
            result.stats.forEach statsLoop@{ stat ->
                val normalizedStat = stat.trim().uppercase().take(3)
                if (normalizedStat.isBlank()) return@statsLoop
                totalBonuses[normalizedStat] = (totalBonuses[normalizedStat] ?: 0) + 1
            }
        }


        return draft.copy(
            baseInfo = draft.baseInfo.copy(
                aggregateStatBonuses = totalBonuses
            )
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\DraftStatsUseCase.kt
