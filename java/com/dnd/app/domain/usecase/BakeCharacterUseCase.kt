// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/BakeCharacterUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ USE CASE - ЭТАП 3 РЕФАКТОРИНГА]
 * "Двигатель Запекания" (Baking Engine).
 * Ответственен за сбор всех СТАТИЧЕСКИХ (гарантированных) владений и снаряжения
 * из расы, класса и предыстории, "запекая" их в `baseInfo`.
 * После этого он очищает невалидные пользовательские выборы и пересчитывает
 * агрегированные бонусы характеристик, возвращая полностью актуализированный черновик.
 */
@Singleton
class BakeCharacterUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val draftStatsUseCase: DraftStatsUseCase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(draft: DraftCharacter): DraftCharacter {
        // 1. Полная очистка
        val staticProfs = mutableSetOf<String>()
        val staticEquip = mutableSetOf<String>()
        val allBackgrounds = libraryRepository.getAllBackgrounds() // Получаем один раз

        // 2. Сбор из Расы
        if (draft.baseInfo.raceIndex.isNotBlank()) {
            libraryRepository.getRaceByIndex(draft.baseInfo.raceIndex)?.let { race ->
                race.baseProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                libraryRepository.getBaseRaceFeatures(race.id).forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }
        if (!draft.baseInfo.subraceIndex.isNullOrBlank()) {
            libraryRepository.getSubraceModelByIndex(draft.baseInfo.subraceIndex)?.let { subrace ->
                subrace.baseProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                libraryRepository.getSubraceFeatures(subrace.index).forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }

        // 3. Сбор из Класса (только для 1-го уровня)
        draft.levelStack.firstOrNull()?.classIndex?.let { classIdx ->
            libraryRepository.getClassEntityByIndex(classIdx)?.let { entity ->
                entity.savingThrowsJson?.let { raw ->
                    runCatching { json.decodeFromString<List<ReferenceJson>>(raw).forEach {
                        staticProfs.add("saving-throw-${it.index.lowercase()}")
                    }}.getOrNull()
                }
                entity.proficienciesJson?.let { raw ->
                    runCatching { json.decodeFromString<List<ReferenceJson>>(raw).forEach {
                        staticProfs.add(normalizeProficiency(it.index))
                    }}.getOrNull()
                }
                entity.startingEquipmentJson?.let { raw ->
                    runCatching { json.decodeFromString<List<ReferenceJson>>(raw).forEach {
                        staticEquip.add(it.index)
                    }}.getOrNull()
                }
            }
        }

        // 4. Сбор из Предыстории
        if (draft.baseInfo.backgroundIndex.isNotBlank()) {
            allBackgrounds.find { it.name == draft.baseInfo.backgroundIndex }?.let { bg ->
                staticEquip.addAll(bg.equipment.filter { it.isNotBlank() })
                bg.features.forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }

        // 5. "Запекание" и очистка
        val intermediateDraft = draft.copy(
            baseInfo = draft.baseInfo.copy(
                staticProficiencies = staticProfs.toList(),
                staticEquipment = staticEquip.toList()
            )
        )
        val prunedDraft = pruneInvalidSelections(intermediateDraft)
        return draftStatsUseCase(prunedDraft)
    }

    private fun normalizeProficiency(index: String): String {
        val lowerIndex = index.lowercase()
        return when {
            lowerIndex.startsWith("skill-") || lowerIndex.startsWith("tool-") || lowerIndex.startsWith("saving-throw-") -> lowerIndex
            DndLocalization.ALL_SKILLS.containsKey(lowerIndex) -> "skill-$lowerIndex"
            // Эвристика для инструментов
            lowerIndex.contains("tools") || lowerIndex.contains("kit") || lowerIndex.contains("supplies") -> "tool-$lowerIndex"
            // Для простых владений типа 'light-armor' оставляем как есть
            else -> lowerIndex
        }
    }

    private fun pruneInvalidSelections(draft: DraftCharacter): DraftCharacter {
        val hardExclusions = draft.getHardExclusions()
        if (hardExclusions.isEmpty()) return draft

        fun pruneResult(result: ChoiceResult, exclusions: Set<String>): ChoiceResult? {
            return when (result) {
                is ChoiceResult.Skills -> {
                    val pruned = result.skillIndexes.filter { it !in exclusions }
                    if (pruned.isNotEmpty()) result.copy(skillIndexes = pruned) else null
                }
                is ChoiceResult.SelectedOptions -> {
                    val pruned = result.items.filter { it !in exclusions }
                    if (pruned.isNotEmpty()) result.copy(items = pruned) else null
                }
                is ChoiceResult.StatBonus -> {
                    val pruned = result.bonuses.filterKeys { it !in exclusions }
                    if (pruned.isNotEmpty()) result.copy(bonuses = pruned) else null
                }
                else -> result
            }
        }

        fun <K> pruneMap(map: Map<K, ChoiceResult>, exclusions: Set<String>): Map<K, ChoiceResult> {
            return buildMap {
                map.forEach { (key, result) ->
                    pruneResult(result, exclusions)?.let { put(key, it) }
                }
            }
        }

        val cleanedRaceSelections = pruneMap(draft.baseInfo.raceSelections, hardExclusions)
        val cleanedBgSelections = pruneMap(draft.baseInfo.backgroundSelections, hardExclusions)

        val cleanedLevelStack = draft.levelStack.map { levelStep ->
            levelStep.copy(selections = pruneMap(levelStep.selections, hardExclusions))
        }

        return draft.copy(
            baseInfo = draft.baseInfo.copy(
                raceSelections = cleanedRaceSelections,
                backgroundSelections = cleanedBgSelections
            ),
            levelStack = cleanedLevelStack
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/BakeCharacterUseCase.kt