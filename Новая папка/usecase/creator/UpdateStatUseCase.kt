// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/UpdateStatUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.creator

import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.rules.DndRules
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ОБНОВЛЕНО v1.31]
 * Движок для изменения базовых характеристик по системе Point Buy.
 * Теперь выполняет только математические операции, не вызывая другие use case.
 */
@Singleton
class UpdateStatUseCase @Inject constructor() {
    operator fun invoke(
        draft: DraftCharacter,
        statKey: String,
        delta: Int
    ): DraftCharacter {
        val currentScores = draft.baseInfo.baseAbilityScores.toMutableMap()
        val currentScore = currentScores[statKey] ?: 8
        val newScore = currentScore + delta

        if (newScore < DndRules.MIN_SCORE || newScore > DndRules.MAX_SCORE) return draft

        val currentSpent = draft.baseInfo.baseAbilityScores.values.sumOf { DndRules.getPointCost(it) }
        val costChange = DndRules.getPointCost(newScore) - DndRules.getPointCost(currentScore)

        if (currentSpent + costChange <= DndRules.MAX_POINTS) {
            currentScores[statKey] = newScore
            return draft.copy(baseInfo = draft.baseInfo.copy(baseAbilityScores = currentScores))
        }

        return draft
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/UpdateStatUseCase.kt