// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\ValidateDraftUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.ChoicePathManager
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.SelectionSource
import com.dnd.app.domain.model.ValidationIssue
import com.dnd.app.domain.model.ValidationReport
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.domain.usecase.ValidateMulticlassPrerequisitesUseCase
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ValidateDraftUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val classFeatureRepository: ClassFeatureRepository,
    private val getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
    private val calculator: DndCalculator,
    private val validateMulticlassPrerequisitesUseCase: ValidateMulticlassPrerequisitesUseCase
) {
    private val TAG = "DND_VALIDATE_UC"

    suspend operator fun invoke(draft: DraftCharacter): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()


        validateRace(draft, issues)
        validateBackground(draft, issues)
        validateClassAndInventory(draft, issues)
        validateStats(draft, issues)
        validateAdvancedRules(draft, issues)

        return ValidationReport(isValid = issues.isEmpty(), issues = issues.distinct())
    }

    private suspend fun validateRace(draft: DraftCharacter, issues: MutableList<ValidationIssue>) {
        val raceIndex = draft.baseInfo.raceIndex
        if (raceIndex.isBlank()) {
            issues.add(ValidationIssue(SelectionSource.RACE, "Раса", 1, 0))
            return
        }

        val race = libraryRepository.getRaceByIndex(raceIndex) ?: return
        val baseFeatures = libraryRepository.getBaseRaceFeatures(race.id)
        val subraceFeatures = draft.baseInfo.subraceIndex?.let {
            libraryRepository.getSubraceFeatures(it)
        } ?: emptyList()

        if (baseFeatures.any { it.isSubraceSelector } && draft.baseInfo.subraceIndex.isNullOrBlank()) {
            issues.add(ValidationIssue(SelectionSource.RACE, "Разновидность расы", 1, 0))
        }

        validateChoicesRecursively(baseFeatures + subraceFeatures, draft.baseInfo.raceSelections, SelectionSource.RACE, 0, issues)
    }

    private suspend fun validateBackground(draft: DraftCharacter, issues: MutableList<ValidationIssue>) {
        if (draft.name.isBlank()) issues.add(ValidationIssue(SelectionSource.BACKGROUND, "Имя персонажа", 1, 3))
        if (draft.baseInfo.gender.isBlank()) issues.add(ValidationIssue(SelectionSource.BACKGROUND, "Пол", 1, 3))

        val bgIndex = draft.baseInfo.backgroundIndex
        if (bgIndex.isBlank()) {
            issues.add(ValidationIssue(SelectionSource.BACKGROUND, "Предыстория", 1, 3))
            return
        }

        val background = libraryRepository.getBackgroundByIndex(bgIndex) ?: return
        validateChoicesRecursively(background.features, draft.baseInfo.backgroundSelections, SelectionSource.BACKGROUND, 3, issues)
    }

    private suspend fun validateClassAndInventory(draft: DraftCharacter, issues: MutableList<ValidationIssue>) {
        val levelStep = draft.levelStack.firstOrNull()
        if (levelStep == null || levelStep.classIndex.isBlank()) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Класс", 1, 1))
            return
        }

        val classMetadata = draft.levelStack.map { it.classIndex }.distinct().mapNotNull { idx ->
            classFeatureRepository.getClassEntity(idx)?.let { idx to it }
        }.toMap()

        val abilityModifier = calculator.calculateRelevantAbilityModifier(draft, classMetadata)
        val currentStats = resolveCurrentStats(draft)
        collectMulticlassTargets(draft).forEach { classIndex ->
            issues.addAll(validateMulticlassPrerequisitesUseCase(draft, classIndex, currentStats))
        }

        val progressionData = getClassProgressionDataUseCase(
            draft = draft,
            classIndex = levelStep.classIndex,
            level = 1,
            subclassIndex = levelStep.subclassIndex,
            abilityModifier = abilityModifier,
            additionalIndexes = draft.baseInfo.staticEquipment,
            proficiencyProvider = { draft.getAllProficienciesWithLevels() },
            editingLevelIndex = 0
        )

        val partitioned = progressionData.partitionedFeatures
        if (partitioned.subclassChoiceFeature != null && levelStep.subclassIndex.isNullOrBlank()) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Подкласс", 1, 1))
        }

        val classTabFeatures = partitioned.classSkillFeatures.toMutableList()
        progressionData.aggregatedSpellFeature?.let { classTabFeatures.add(it) }

        validateChoicesRecursively(classTabFeatures, levelStep.selections, SelectionSource.CLASS, 1, issues)
        validateChoicesRecursively(partitioned.inventoryChoiceFeatures, draft.baseInfo.inventorySelections, SelectionSource.INVENTORY, 4, issues)
    }


    private suspend fun validateChoicesRecursively(
        features: List<Feature>,
        selections: Map<String, ChoiceResult>,
        source: SelectionSource,
        tabIndex: Int,
        issues: MutableList<ValidationIssue>,
        parentKey: String? = null,
        suppressIndexAppend: Boolean = false,
        depth: Int = 0
    ) {
        if (depth > 12) {
            Log.e(TAG, "Validation recursion limit reached at depth $depth")
            return
        }

        for (feature in features) {
            feature.choices.forEachIndexed { cIdx, choice ->


                val currentChoiceKey = if (parentKey == null) {
                    ChoicePathManager.createIndexedKey(source, feature.index, cIdx)
                } else {
                    if (suppressIndexAppend && cIdx == 0) parentKey
                    else ChoicePathManager.append(parentKey, "", cIdx)
                }

                if (choice is FeatureChoiceDomain.InvalidChoice) {
                    issues.add(ValidationIssue(source, "Ошибка: ${feature.name}", 1, tabIndex))
                    return@forEachIndexed
                }

                val selection = selections[currentChoiceKey]
                val isTransparent = (choice as? FeatureChoiceDomain.SelectOption)?.isTransparent == true

                if (isTransparent && choice is FeatureChoiceDomain.SelectOption) {

                    choice.options.forEach { opt ->
                        opt.subChoice?.let { sub ->
                            val subFeature = Feature(
                                id = -1, index = opt.id, name = opt.label,
                                description = "", choices = listOf(sub)
                            )

                            val nextPath = ChoicePathManager.append(currentChoiceKey, opt.id, 0)
                            validateChoicesRecursively(
                                features = listOf(subFeature),
                                selections = selections,
                                source = source,
                                tabIndex = tabIndex,
                                issues = issues,
                                parentKey = nextPath,
                                suppressIndexAppend = true,
                                depth = depth + 1
                            )
                        }
                    }
                } else {

                    val required = choice.count
                    val actual = when (selection) {
                        null -> 0
                        is ChoiceResult.Spells -> selection.spellIndexes.size
                        is ChoiceResult.SelectedOptions -> selection.items.size
                        is ChoiceResult.StatBonus -> selection.stats.size
                        is ChoiceResult.Skills -> selection.skillIndexes.size
                        else -> 0
                    }

                    if (actual < required) {
                        issues.add(ValidationIssue(source, feature.name, (required - actual), tabIndex))
                    }


                    if (selection is ChoiceResult.SelectedOptions) {
                        selection.items.forEach { selectedId ->
                            val resolvedFeat = libraryRepository.getFeatureByIndex(selectedId)
                            val staticSub = (choice as? FeatureChoiceDomain.SelectOption)?.options?.find { it.id == selectedId }?.subChoice
                            val nextPath = ChoicePathManager.append(currentChoiceKey, selectedId, 0)

                            if (resolvedFeat != null) {

                                validateChoicesRecursively(
                                    features = listOf(resolvedFeat),
                                    selections = selections,
                                    source = source,
                                    tabIndex = tabIndex,
                                    issues = issues,
                                    parentKey = nextPath,
                                    suppressIndexAppend = false,
                                    depth = depth + 1
                                )
                            } else if (staticSub != null) {

                                val proxy = Feature(id = -1, index = selectedId, name = selectedId, description = "", choices = listOf(staticSub))
                                validateChoicesRecursively(
                                    features = listOf(proxy),
                                    selections = selections,
                                    source = source,
                                    tabIndex = tabIndex,
                                    issues = issues,
                                    parentKey = nextPath,
                                    suppressIndexAppend = true,
                                    depth = depth + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateStats(draft: DraftCharacter, issues: MutableList<ValidationIssue>) {
        val totalCost = draft.baseInfo.baseAbilityScores.values.sumOf { DndRules.getPointCost(it) }
        if (totalCost != DndRules.MAX_POINTS) {
            val label = if (totalCost > DndRules.MAX_POINTS) "Превышен лимит очков" else "Очки не распределены"
            issues.add(ValidationIssue(SelectionSource.CLASS, label, 1, 2))
        }
    }

    private fun validateAdvancedRules(draft: DraftCharacter, issues: MutableList<ValidationIssue>) {
        val allSelections = (draft.baseInfo.raceSelections.values + draft.baseInfo.backgroundSelections.values +
                draft.baseInfo.inventorySelections.values + draft.levelStack.flatMap { it.selections.values })

        val chosenSpells = allSelections.filterIsInstance<ChoiceResult.Spells>().flatMap { it.spellIndexes }
        val autoSpells = draft.baseInfo.staticSpells + draft.levelStack.flatMap { it.autoSpells }

        val totalSpells = chosenSpells + autoSpells
        if (totalSpells.size != totalSpells.distinct().size) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Дубликат заклинания", 1, 1))
        }
    }

    private fun resolveCurrentStats(draft: DraftCharacter): Map<String, Int> {
        return draft.baseInfo.baseAbilityScores.mapValues { (stat, value) ->
            value + (draft.baseInfo.aggregateStatBonuses[stat] ?: 0)
        }
    }

    private fun collectMulticlassTargets(draft: DraftCharacter): Set<String> {
        val firstOccurrence = mutableMapOf<String, Int>()
        draft.levelStack.forEachIndexed { index, step ->
            val classIndex = step.classIndex
            if (classIndex.isBlank()) return@forEachIndexed
            firstOccurrence.putIfAbsent(classIndex, index)
        }
        return firstOccurrence.filterValues { it > 0 }.keys
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\ValidateDraftUseCase.kt
