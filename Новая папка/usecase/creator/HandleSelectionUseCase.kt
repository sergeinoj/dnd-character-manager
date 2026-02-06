// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/HandleSelectionUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.creator

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.creator.HandleSelectionResult
import javax.inject.Inject
import javax.inject.Singleton

enum class SelectionSource {
    RACE, CLASS, BACKGROUND, INVENTORY, BIO, LEVEL_UP
}

/**
 * [ОБНОВЛЕНО v1.31]
 * Унифицированный обработчик всех выборов пользователя.
 * Реализована логика слияния (merge) для `StatBonus`.
 */
@Singleton
class HandleSelectionUseCase @Inject constructor() {
    operator fun invoke(
        source: SelectionSource,
        key: String,
        result: ChoiceResult,
        draft: DraftCharacter
    ): HandleSelectionResult {

        val newDraft = when (source) {
            SelectionSource.RACE -> {
                val newSelections = mergeSelections(draft.baseInfo.raceSelections, key, result)
                draft.copy(baseInfo = draft.baseInfo.copy(raceSelections = newSelections))
            }
            SelectionSource.CLASS -> {
                val stack = draft.levelStack.toMutableList()
                if (stack.isNotEmpty()) {
                    val newSelections = mergeSelections(stack[0].selections, key, result)
                    stack[0] = stack[0].copy(selections = newSelections)
                }
                draft.copy(levelStack = stack)
            }
            SelectionSource.BACKGROUND -> {
                val newSelections = mergeSelections(draft.baseInfo.backgroundSelections, key, result)
                draft.copy(baseInfo = draft.baseInfo.copy(backgroundSelections = newSelections))
            }
            SelectionSource.INVENTORY -> {
                val newSelections = mergeSelections(draft.baseInfo.inventorySelections, key, result)
                draft.copy(baseInfo = draft.baseInfo.copy(inventorySelections = newSelections))
            }
            else -> draft
        }

        var featToLoad: String? = null
        if (source == SelectionSource.RACE && !key.contains("_") && result is ChoiceResult.SelectedOptions) {
            val selection = result.items.firstOrNull()
            if (selection?.startsWith("feat-") == true) {
                featToLoad = selection
            }
        }

        return HandleSelectionResult(newDraft, featToLoad)
    }

    /**
     * [НОВЫЙ МЕТОД v1.31]
     * Обрабатывает слияние результатов выбора, особенно для `StatBonus`.
     */
    private fun mergeSelections(
        currentMap: Map<String, ChoiceResult>,
        key: String,
        newResult: ChoiceResult
    ): Map<String, ChoiceResult> {
        val mutableMap = currentMap.toMutableMap()
        val existingResult = currentMap[key]

        if (existingResult is ChoiceResult.StatBonus && newResult is ChoiceResult.StatBonus) {
            // Логика слияния: объединяем карты бонусов.
            val mergedBonuses = existingResult.bonuses + newResult.bonuses
            mutableMap[key] = ChoiceResult.StatBonus(mergedBonuses)
        } else {
            // Стандартное поведение: замена.
            mutableMap[key] = newResult
        }

        return mutableMap
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/HandleSelectionUseCase.kt