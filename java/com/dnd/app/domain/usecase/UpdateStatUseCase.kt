// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/UpdateStatUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.rules.DndRules
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ USE CASE]
 * Изолирует логику изменения базовых характеристик персонажа (Point-Buy).
 * Является единственным источником истины для правил D&D, касающихся распределения очков.
 *
 * 1. Валидирует новое значение характеристики (в пределах MIN/MAX).
 * 2. Валидирует общую стоимость очков (не больше MAX_POINTS).
 * 3. Если валидация пройдена, обновляет `baseAbilityScores`.
 * 4. Вызывает `draftStatsUseCase` для пересчета `aggregateStatBonuses`.
 * 5. Возвращает новый, обновленный `DraftCharacter` или исходный, если изменение невозможно.
 */
@Singleton
class UpdateStatUseCase @Inject constructor(
    private val draftStatsUseCase: DraftStatsUseCase
) {
    suspend operator fun invoke(draft: DraftCharacter, statKey: String, diff: Int): DraftCharacter {
        val currentScores = draft.baseInfo.baseAbilityScores
        val currentScore = currentScores[statKey] ?: DndRules.MIN_SCORE
        val newScore = currentScore + diff

        // Проверка 1: Новое значение не должно выходить за пределы 8-15
        if (newScore < DndRules.MIN_SCORE || newScore > DndRules.MAX_SCORE) {
            return draft
        }

        val currentSpent = currentScores.values.sumOf { DndRules.getPointCost(it) }
        val costChange = DndRules.getPointCost(newScore) - DndRules.getPointCost(currentScore)

        // Проверка 2: Новая стоимость не должна превышать максимум
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

        // Финальный шаг: пересчитываем все бонусы, зависящие от характеристик
        return draftStatsUseCase(updatedDraft)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/UpdateStatUseCase.kt