// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/HandleSelectionUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.LevelStep
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ USE CASE - ЭТАП 6]
 * Централизует логику обработки пользовательских выборов в черновике.
 * Главная ответственность - управление зависимыми выборами. Например, если пользователь
 * меняет основную черту (feat), этот UseCase автоматически очищает все под-выборы,
 * которые были сделаны для *предыдущей* черты, предотвращая сохранение невалидных данных.
 */
@Singleton
class HandleSelectionUseCase @Inject constructor() {

    enum class SelectionSource {
        RACE, CLASS, BACKGROUND, INVENTORY
    }

    suspend operator fun invoke(
        draft: DraftCharacter,
        source: SelectionSource,
        key: String,
        result: ChoiceResult
    ): DraftCharacter {
        return when (source) {
            SelectionSource.RACE -> handleRaceSelection(draft, key, result)
            SelectionSource.CLASS -> handleClassSelection(draft, key, result)
            SelectionSource.BACKGROUND -> handleBackgroundSelection(draft, key, result)
            SelectionSource.INVENTORY -> handleInventorySelection(draft, key, result)
        }
    }

    private fun handleRaceSelection(draft: DraftCharacter, key: String, result: ChoiceResult): DraftCharacter {
        val oldSelections = draft.baseInfo.raceSelections
        val newSelections = oldSelections.toMutableMap()

        // Проверяем, является ли это выбором основной черты
        val isPrimaryFeatSelection = !key.contains("_") &&
                result is ChoiceResult.SelectedOptions &&
                result.items.firstOrNull()?.startsWith("feat-") == true

        if (isPrimaryFeatSelection) {
            val oldFeatIndex = (oldSelections[key] as? ChoiceResult.SelectedOptions)?.items?.firstOrNull()
            val newFeatIndex = (result as ChoiceResult.SelectedOptions).items.first()

            // Если черта сменилась, удаляем все старые под-выборы
            if (oldFeatIndex != null && oldFeatIndex != newFeatIndex) {
                newSelections.keys.removeIf { it.startsWith("${key}_${oldFeatIndex}") }
            }
        }

        newSelections[key] = result

        return draft.copy(
            baseInfo = draft.baseInfo.copy(raceSelections = newSelections)
        )
    }

    private fun handleClassSelection(draft: DraftCharacter, key: String, result: ChoiceResult): DraftCharacter {
        val stack = draft.levelStack.toMutableList()
        if (stack.isEmpty()) return draft // Не должно происходить, но для безопасности

        val currentLevelStep = stack.first()
        val newSelections = currentLevelStep.selections.toMutableMap()
        newSelections[key] = result
        stack[0] = currentLevelStep.copy(selections = newSelections)

        return draft.copy(levelStack = stack)
    }

    private fun handleBackgroundSelection(draft: DraftCharacter, key: String, result: ChoiceResult): DraftCharacter {
        val newSelections = draft.baseInfo.backgroundSelections.toMutableMap()
        newSelections[key] = result
        return draft.copy(
            baseInfo = draft.baseInfo.copy(backgroundSelections = newSelections)
        )
    }

    private fun handleInventorySelection(draft: DraftCharacter, key: String, result: ChoiceResult): DraftCharacter {
        val newSelections = draft.baseInfo.inventorySelections.toMutableMap()
        newSelections[key] = result
        return draft.copy(
            baseInfo = draft.baseInfo.copy(inventorySelections = newSelections)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/HandleSelectionUseCase.kt