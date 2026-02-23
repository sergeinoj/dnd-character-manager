// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\UpdateStatUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.rules.DndRules
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UpdateStatUseCase @Inject constructor(
    private val draftStatsUseCase: DraftStatsUseCase
) {
    suspend operator fun invoke(draft: DraftCharacter, statKey: String, diff: Int): DraftCharacter {
        val currentScores = draft.baseInfo.baseAbilityScores
        val currentScore = currentScores[statKey] ?: DndRules.MIN_SCORE
        val newScore = currentScore + diff


        if (newScore < DndRules.MIN_SCORE || newScore > DndRules.MAX_SCORE) {
            return draft
        }

        val currentSpent = currentScores.values.sumOf { DndRules.getPointCost(it) }
        val costChange = DndRules.getPointCost(newScore) - DndRules.getPointCost(currentScore)


        if (currentSpent + costChange > DndRules.MAX_POINTS) {
            return draft
        }

        val updatedScores = currentScores.toMutableMap().apply {
            this[statKey] = newScore
        }

        val updatedDraft = draft.copy(
            baseInfo = draft.baseInfo.copy(
                baseAbilityScores = updatedScores
            )
        )


        return draftStatsUseCase(updatedDraft)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\UpdateStatUseCase.kt