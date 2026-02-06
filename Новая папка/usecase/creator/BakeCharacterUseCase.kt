// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/BakeCharacterUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.creator

import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.DraftStatsUseCase
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ОБНОВЛЕНО v1.31.1]
 * Движок "Запекания" (Bake).
 * Синхронизирует статические данные из БД (раса, класс, фон) с черновиком.
 * Вычисляет `staticProficiencies` и `staticEquipment`, а затем очищает
 * пользовательские выборы, которые стали избыточными, используя восстановленный `getHardExclusions`.
 */
@Singleton
class BakeCharacterUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val draftStatsUseCase: DraftStatsUseCase,
    private val json: Json
) {
    suspend operator fun invoke(draft: DraftCharacter, allBackgrounds: List<Background>): DraftCharacter {
        val staticProfs = mutableSetOf<String>()
        val staticEquip = mutableSetOf<String>()

        // 1. Сбор из Расы и Подрасы
        if (draft.baseInfo.raceIndex.isNotBlank()) {
            libraryRepository.getRaceByIndex(draft.baseInfo.raceIndex)?.let { race ->
                race.baseProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                libraryRepository.getBaseRaceFeatures(race.id).forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }
        draft.baseInfo.subraceIndex?.let { subraceIndex ->
            libraryRepository.getSubraceModelByIndex(subraceIndex)?.let { subrace ->
                subrace.baseProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                libraryRepository.getSubraceFeatures(subrace.index).forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }

        // 2. Сбор из Класса (только для 1-го уровня)
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

        // 3. Сбор из Предыстории
        if (draft.baseInfo.backgroundIndex.isNotBlank()) {
            allBackgrounds.find { it.name == draft.baseInfo.backgroundIndex }?.let { bg ->
                staticEquip.addAll(bg.equipment.filter { it.isNotBlank() })
                bg.features.forEach { feat ->
                    feat.grantedProficiencies.forEach { staticProfs.add(normalizeProficiency(it)) }
                }
            }
        }

        val intermediateDraft = draft.copy(
            baseInfo = draft.baseInfo.copy(
                staticProficiencies = staticProfs.toList(),
                staticEquipment = staticEquip.toList()
            )
        )
        // [ИСПРАВЛЕНО] Пересчет статов теперь происходит ПОСЛЕ очистки
        val statRecalculatedDraft = draftStatsUseCase(intermediateDraft)
        return pruneInvalidSelections(statRecalculatedDraft)
    }

    private fun pruneInvalidSelections(draft: DraftCharacter): DraftCharacter {
        val hardExclusions = draft.getHardExclusions()
        if (hardExclusions.isEmpty()) return draft

        fun pruneResult(result: ChoiceResult, exclusions: Set<String>): ChoiceResult? {
            return when (result) {
                is ChoiceResult.Skills -> {
                    val pruned = result.skillIndexes.filter { normalizeProficiency(it) !in exclusions }
                    if (pruned.size < result.skillIndexes.size) result.copy(skillIndexes = pruned) else result
                }
                is ChoiceResult.SelectedOptions -> {
                    val pruned = result.items.filter { normalizeProficiency(it) !in exclusions }
                    if (pruned.size < result.items.size) result.copy(items = pruned) else result
                }
                is ChoiceResult.StatBonus -> {
                    val pruned = result.bonuses.filterKeys { it !in exclusions }
                    if (pruned.size < result.bonuses.size) result.copy(bonuses = pruned) else result
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

    private fun normalizeProficiency(index: String): String {
        val lowerIndex = index.lowercase()
        return when {
            lowerIndex.startsWith("skill-") || lowerIndex.startsWith("tool-") || lowerIndex.startsWith("saving-throw-") -> lowerIndex
            DndLocalization.ALL_SKILLS.containsKey(lowerIndex) -> "skill-$lowerIndex"
            else -> lowerIndex
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/BakeCharacterUseCase.kt