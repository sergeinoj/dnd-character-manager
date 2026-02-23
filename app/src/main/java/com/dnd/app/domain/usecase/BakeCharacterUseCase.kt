// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\BakeCharacterUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.data.model.ClassSpecificJson
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.data.repository.mapper.ProficiencyMapper
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BakeCharacterUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val draftStatsUseCase: DraftStatsUseCase,
    private val proficiencyMapper: ProficiencyMapper,
    private val classFeatureRepository: ClassFeatureRepository,
    private val calculator: DndCalculator,
    private val json: Json
) {

    suspend operator fun invoke(draft: DraftCharacter): DraftCharacter = coroutineScope {
        var mutableDraft = draft

        if (mutableDraft.baseInfo.startingClassIndex.isBlank()) {
            mutableDraft.levelStack.firstOrNull()?.classIndex?.let { firstClass ->
                mutableDraft = mutableDraft.copy(
                    baseInfo = mutableDraft.baseInfo.copy(startingClassIndex = firstClass)
                )
            }
        }

        val raceDataDef = async { fetchRaceStatics(mutableDraft) }
        val classDataDef = async { fetchClassStatics(mutableDraft) }
        val bgDataDef = async { fetchBackgroundStatics(mutableDraft) }
        val levelStackDataDef = async { fetchLevelStackStatics(mutableDraft) }
        val (rProfs, rSpells) = raceDataDef.await()
        val (cProfs, cEquip) = classDataDef.await()
        val (bProfs, bEquip, bSpells, bGold) = bgDataDef.await()
        val levelStackSpells = levelStackDataDef.await()

        val intermediateDraft = mutableDraft.copy(
            baseInfo = mutableDraft.baseInfo.copy(
                staticProficiencies = (rProfs + cProfs + bProfs)
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id },
                staticEquipment = (cEquip + bEquip)
                    .filter { it.isNotBlank() }
                    .distinct(),


                staticSpells = (rSpells + bSpells + levelStackSpells)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList(),
                startingGold = bGold
            )
        )

        val exhaustiveStatBonuses = calculateExhaustiveStatBonuses(intermediateDraft)

        val finalBaseWithBonuses = intermediateDraft.baseInfo.copy(
            aggregateStatBonuses = exhaustiveStatBonuses
        )

        val statsCalculatedDraft = draftStatsUseCase(intermediateDraft.copy(baseInfo = finalBaseWithBonuses))

        val proficiencyPrunedDraft = pruneProficiencySelections(statsCalculatedDraft)

        enforcePreparationLimits(proficiencyPrunedDraft)
    }

    private suspend fun calculateExhaustiveStatBonuses(draft: DraftCharacter): Map<String, Int> {
        val totalBonuses = mutableMapOf<String, Int>()
        val allActiveIds = mutableSetOf<String>()

        draft.baseInfo.staticProficiencies.forEach { allActiveIds.add(it.id) }

        val allSelectionValues = (draft.baseInfo.raceSelections.values +
                draft.baseInfo.backgroundSelections.values +
                draft.levelStack.flatMap { it.selections.values })

        allSelectionValues.filterIsInstance<ChoiceResult.SelectedOptions>()
            .flatMap { it.items }
            .forEach { allActiveIds.add(it) }

        for (id in allActiveIds) {
            val feature = libraryRepository.getFeatureByIndex(id) ?: continue
            val refJson = feature.referenceJson ?: continue

            runCatching {
                val refObj = json.parseToJsonElement(refJson).jsonObject
                refObj["stat_bonus"]?.jsonObject?.forEach { (stat, bonus) ->
                    val statKey = stat.uppercase().take(3)
                    val bonusValue = bonus.jsonPrimitive.content.toIntOrNull() ?: 0
                    totalBonuses[statKey] = (totalBonuses[statKey] ?: 0) + bonusValue
                }
            }
        }

        allSelectionValues.filterIsInstance<ChoiceResult.StatBonus>()
            .forEach { result ->
                result.stats.forEach statsLoop@{ stat ->
                    val statKey = stat.trim().uppercase().take(3)
                    if (statKey.isBlank()) return@statsLoop
                    totalBonuses[statKey] = (totalBonuses[statKey] ?: 0) + 1
                }
            }

        return totalBonuses
    }

    private suspend fun fetchRaceStatics(draft: DraftCharacter) = coroutineScope {
        val profs = mutableSetOf<StaticProficiency>()
        val spells = mutableSetOf<String>()
        if (draft.baseInfo.raceIndex.isNotBlank()) {
            libraryRepository.getRaceFullData(draft.baseInfo.raceIndex)?.let { data ->
                data.race.baseProficiencies.forEach { runCatching { profs.add(proficiencyMapper.map(it)) } }
                data.features.forEach { f ->
                    f.grantedProficiencies.forEach { runCatching { profs.add(proficiencyMapper.map(it)) } }
                    spells.addAll(f.embeddedSpells.map { it.index })
                }
            }
        }
        draft.baseInfo.subraceIndex?.let { libraryRepository.getSubraceModelByIndex(it) }?.let { sub ->
            sub.baseProficiencies.forEach { runCatching { profs.add(proficiencyMapper.map(it)) } }
            libraryRepository.getSubraceFeatures(sub.index).forEach { f ->
                f.grantedProficiencies.forEach { runCatching { profs.add(proficiencyMapper.map(it)) } }
                spells.addAll(f.embeddedSpells.map { it.index })
            }
        }
        profs to spells
    }


    private suspend fun fetchClassStatics(draft: DraftCharacter) = coroutineScope {
        val profs = mutableSetOf<StaticProficiency>()
        val equip = mutableSetOf<String>()
        val startingClass = draft.baseInfo.startingClassIndex
        if (startingClass.isBlank()) return@coroutineScope profs to equip
        val entity = libraryRepository.getClassEntityByIndex(startingClass)
        entity?.let { e ->
            e.savingThrowsJson?.let { r ->
                json.decodeFromString<List<ReferenceJson>>(r).forEach {
                    profs.add(proficiencyMapper.mapRaw(it.index, ProficiencyKind.SAVING_THROW))
                }
            }
            e.proficienciesJson?.let { r ->
                json.decodeFromString<List<ReferenceJson>>(r).forEach {
                    profs.add(proficiencyMapper.map(it.index))
                }
            }
            e.startingEquipmentJson?.let { r ->
                json.decodeFromString<List<ReferenceJson>>(r).forEach {
                    equip.add(it.index)
                }
            }
        }
        profs to equip
    }

    private suspend fun fetchBackgroundStatics(draft: DraftCharacter): BackgroundStatics = coroutineScope {
        val profs = mutableSetOf<StaticProficiency>()
        val equip = mutableSetOf<String>()
        val spells = mutableSetOf<String>()
        var gold = 0
        if (draft.baseInfo.backgroundIndex.isNotBlank()) {
            libraryRepository.getBackgroundByIndex(draft.baseInfo.backgroundIndex)?.let { bg ->
                equip.addAll(bg.equipment)
                bg.staticProficiencies.forEach { runCatching { profs.add(proficiencyMapper.map(it)) } }
                bg.features.forEach { f ->
                    f.grantedProficiencies.forEach { p -> runCatching { profs.add(proficiencyMapper.map(p)) } }
                    spells.addAll(f.embeddedSpells.map { it.index })
                }
                gold = bg.startingGold
            }
        }
        BackgroundStatics(profs, equip, spells, gold)
    }




    private suspend fun fetchLevelStackStatics(draft: DraftCharacter): Set<String> = coroutineScope {
        val collectedSpells = mutableSetOf<String>()
        val classLevelsMap = mutableMapOf<String, Int>()


        val deferredFeatures = draft.levelStack.mapIndexed { index, step ->
            val classIdx = step.classIndex
            val currentLevel = (classLevelsMap[classIdx] ?: 0) + 1
            classLevelsMap[classIdx] = currentLevel

            async {
                libraryRepository.getClassFeaturesForLevel(
                    classIndex = classIdx,
                    level = currentLevel,
                    subclassIndex = step.subclassIndex,
                    isGenesis = index == 0
                )
            }
        }

        deferredFeatures.awaitAll().forEach { featuresForLevel ->
            val allFeatures = featuresForLevel.baseClassFeatures +
                    featuresForLevel.selectedSubclassFeatures +
                    listOfNotNull(featuresForLevel.subclassChoiceFeature)

            allFeatures.forEach { feature ->
                collectedSpells.addAll(feature.embeddedSpells.map { it.index })
            }
        }

        collectedSpells
    }

    private fun pruneProficiencySelections(draft: DraftCharacter): DraftCharacter {
        val staticIds = draft.baseInfo.staticProficiencies.map { it.id }.toSet()
        if (staticIds.isEmpty()) return draft

        val newBase = draft.baseInfo.copy(
            raceSelections = cleanSelectionsMap(draft.baseInfo.raceSelections, staticIds),
            backgroundSelections = cleanSelectionsMap(draft.baseInfo.backgroundSelections, staticIds)
        )
        val newStack = draft.levelStack.map { it.copy(selections = cleanSelectionsMap(it.selections, staticIds)) }
        return draft.copy(levelStack = newStack, baseInfo = newBase)
    }

    private fun cleanSelectionsMap(selections: Map<String, ChoiceResult>, staticIds: Set<String>): Map<String, ChoiceResult> {
        val newMap = selections.toMutableMap()
        var changed = false
        for ((key, res) in selections) {


            if (key.count { it == '.' } > 5) continue

            when (res) {
                is ChoiceResult.SelectedOptions -> {
                    if (res.targetProficiencyLevel == 2) continue
                    val filtered = res.items.filter { it !in staticIds }
                    if (filtered.size != res.items.size) {
                        changed = true
                        if (filtered.isEmpty()) newMap.remove(key) else newMap[key] = res.copy(items = filtered)
                    }
                }
                is ChoiceResult.Skills -> {
                    val filtered = res.skillIndexes.filter { it !in staticIds }
                    if (filtered.size != res.skillIndexes.size) {
                        changed = true
                        if (filtered.isEmpty()) newMap.remove(key) else newMap[key] = res.copy(skillIndexes = filtered)
                    }
                }
                else -> {}
            }
        }
        return if (changed) newMap else selections
    }

    private suspend fun enforcePreparationLimits(draft: DraftCharacter): DraftCharacter {
        val classLevelsEncountered = mutableMapOf<String, Int>()
        val newStack = draft.levelStack.map { step ->
            val lvl = classLevelsEncountered.getOrDefault(step.classIndex, 0) + 1
            classLevelsEncountered[step.classIndex] = lvl

            val progRow = classFeatureRepository.getProgressionForLevel(step.classIndex, lvl).firstOrNull()
            val prepRule = progRow?.classSpecificJson?.let { runCatching { json.decodeFromString<ClassSpecificJson>(it).preparationRule }.getOrNull() }
                ?: return@map step

            val entryToUpdate = step.selections.entries.find { (k, v) ->
                ChoicePathManager.isPreparedSpellPath(k) &&
                        !k.contains("cantrip", ignoreCase = true) &&
                        !k.contains(DndConstants.VirtualKeys.INITIAL_SPELLS, ignoreCase = true) &&
                        v is ChoiceResult.Spells
            } ?: return@map step

            val statMod = calculator.calculateModifier((draft.baseInfo.baseAbilityScores[prepRule.stat] ?: 10) + (draft.baseInfo.aggregateStatBonuses[prepRule.stat] ?: 0))
            val maxSpells = calculator.resolvePreparationFormula(prepRule.formula, lvl, statMod, prepRule.minLimit)

            val currentSpells = (entryToUpdate.value as ChoiceResult.Spells).spellIndexes
            if (currentSpells.size <= maxSpells) return@map step

            val newSelections = step.selections.toMutableMap()
            newSelections[entryToUpdate.key] = ChoiceResult.Spells(currentSpells.take(maxSpells))
            step.copy(selections = newSelections)
        }
        return draft.copy(levelStack = newStack)
    }
}

internal data class BackgroundStatics(
    val profs: Set<StaticProficiency>,
    val equip: Set<String>,
    val spells: Set<String>,
    val gold: Int
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\BakeCharacterUseCase.kt
