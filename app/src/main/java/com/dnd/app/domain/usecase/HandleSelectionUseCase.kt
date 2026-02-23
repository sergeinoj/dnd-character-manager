// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\HandleSelectionUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoicePathManager
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.SelectionSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleSelectionUseCase @Inject constructor() {


    suspend operator fun invoke(
        draft: DraftCharacter,
        source: SelectionSource,
        key: String,
        result: ChoiceResult,
        levelIndex: Int? = null
    ): DraftCharacter {
        val canonicalKey = normalizeKey(source, key)

        return when (source) {
            SelectionSource.RACE -> {
                val newSelections = draft.baseInfo.raceSelections.toMutableMap()
                updateAndPurge(newSelections, canonicalKey, result)
                draft.copy(baseInfo = draft.baseInfo.copy(raceSelections = newSelections))
            }
            SelectionSource.CLASS -> {
                val stack = draft.levelStack.toMutableList()
                if (stack.isEmpty()) return draft
                val targetIndex = levelIndex ?: 0
                if (targetIndex in stack.indices) {
                    val currentLevelStep = stack[targetIndex]
                    val newSelections = currentLevelStep.selections.toMutableMap()
                    updateAndPurge(newSelections, canonicalKey, result)
                    stack[targetIndex] = currentLevelStep.copy(selections = newSelections)
                    draft.copy(levelStack = stack)
                } else {
                    draft
                }
            }
            SelectionSource.BACKGROUND -> {
                val newSelections = draft.baseInfo.backgroundSelections.toMutableMap()
                updateAndPurge(newSelections, canonicalKey, result)
                draft.copy(baseInfo = draft.baseInfo.copy(backgroundSelections = newSelections))
            }
            SelectionSource.INVENTORY -> {
                val newSelections = draft.baseInfo.inventorySelections.toMutableMap()
                updateAndPurge(newSelections, canonicalKey, result)
                draft.copy(baseInfo = draft.baseInfo.copy(inventorySelections = newSelections))
            }
        }
    }


    private fun normalizeKey(source: SelectionSource, key: String): String {
        val prefix = "${source.name}."
        val body = if (key.startsWith(prefix, ignoreCase = true)) key.substring(prefix.length) else key

        if (source == SelectionSource.INVENTORY) {
            return "$prefix${body.lowercase()}"
        }

        val segments = body.split(".")
        val normalizedSegments = segments.map { segment ->
            if (segment.contains(ChoicePathManager.INDEX_MARKER)) segment.lowercase()
            else "${segment.lowercase()}${ChoicePathManager.INDEX_MARKER}0"
        }

        return "$prefix${normalizedSegments.joinToString(".")}"
    }

    private fun updateAndPurge(
        map: MutableMap<String, ChoiceResult>,
        key: String,
        result: ChoiceResult
    ) {
        // RALPH FIX: avoid aggressive purging of child keys when the selection is a simple removal note.
        // If result is a removal marker, keep existing child entries intact to avoid losing nested state
        // that may not have been created yet or may belong to other concurrent UI branches.
        if (result is ChoiceResult.Note) {
            map[key] = result
            return
        }

        map.keys.removeIf { ChoicePathManager.isChildOf(key, it) }
        map[key] = result
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\HandleSelectionUseCase.kt