// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\level_up\ValidateLevelUpUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.level_up

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.usecase.GetClassProgressionDataUseCase
import com.dnd.app.domain.usecase.ValidateMulticlassPrerequisitesUseCase
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidateLevelUpUseCase @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
    private val calculator: DndCalculator,
    private val validateMulticlassPrerequisitesUseCase: ValidateMulticlassPrerequisitesUseCase
) {
    suspend operator fun invoke(draft: DraftCharacter, levelIndex: Int): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val levelStep = draft.levelStack.getOrNull(levelIndex)

        if (levelStep == null || levelStep.classIndex.isBlank()) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Данные уровня", 1, 1))
            return ValidationReport(false, issues)
        }

        if (levelStep.hpIncrease <= 0) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Прирост хитов", 1, 1))
        }

        val classLevelInStack = draft.levelStack.take(levelIndex + 1).count { it.classIndex == levelStep.classIndex }
        val currentStats = resolveCurrentStats(draft)
        if (levelIndex > 0 && levelStep.classIndex.isNotBlank() &&
            draft.levelStack.take(levelIndex).none { it.classIndex == levelStep.classIndex }
        ) {
            issues.addAll(validateMulticlassPrerequisitesUseCase(draft, levelStep.classIndex, currentStats))
        }

        val classMetadata = draft.levelStack.map { it.classIndex }.distinct().mapNotNull { idx ->
            classFeatureRepository.getClassEntity(idx)?.let { idx to it }
        }.toMap()

        val abilityModifier = calculator.calculateRelevantAbilityModifier(draft, classMetadata)

        val progressionData = getClassProgressionDataUseCase(
            draft = draft,
            classIndex = levelStep.classIndex,
            level = classLevelInStack,
            subclassIndex = levelStep.subclassIndex,
            abilityModifier = abilityModifier,
            proficiencyProvider = { draft.getAllProficienciesWithLevels() },
            editingLevelIndex = levelIndex
        )

        val partitioned = progressionData.partitionedFeatures

        if (partitioned.subclassChoiceFeature != null && levelStep.subclassIndex.isNullOrBlank()) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Подкласс", 1, 1))
        }

        val featuresToValidate = mutableListOf<Feature>()
        featuresToValidate.addAll(partitioned.classSkillFeatures)
        featuresToValidate.addAll(partitioned.inventoryChoiceFeatures)
        progressionData.aggregatedSpellFeature?.let { featuresToValidate.add(it) }

        validateChoicesRecursively(
            features = featuresToValidate,
            selections = levelStep.selections,
            issues = issues
        )

        val activeSpellKeys = collectSpellChoiceKeys(featuresToValidate, SelectionSource.CLASS)
        if (hasDuplicateSpells(draft, levelIndex, activeSpellKeys, abilityModifier)) {
            issues.add(ValidationIssue(SelectionSource.CLASS, "Дубликат заклинания", 1, 1))
        }

        return ValidationReport(isValid = issues.isEmpty(), issues = issues.distinct())
    }

    private suspend fun hasDuplicateSpells(
        draft: DraftCharacter,
        currentLevelIndex: Int,
        activeSpellKeys: Set<String>,
        abilityModifier: Int
    ): Boolean {
        val baseSelections = draft.baseInfo.raceSelections.values +
                draft.baseInfo.backgroundSelections.values +
                draft.baseInfo.inventorySelections.values

        val currentLevelSelections = draft.levelStack.getOrNull(currentLevelIndex)?.selections ?: emptyMap()
        val currentLevelSpellEntries = currentLevelSelections
            .filterKeys { it in activeSpellKeys }
            .toList()
        val currentLevelSpells = currentLevelSpellEntries
            .mapNotNull { (_, value) -> value as? ChoiceResult.Spells }

        val otherLevelSpellEntries = mutableListOf<Triple<Int, String, ChoiceResult>>()
        draft.levelStack.forEachIndexed { idx, step ->
            if (idx == currentLevelIndex) return@forEachIndexed
            val keys = resolveActiveSpellKeysForLevel(draft, idx, step, abilityModifier)
            step.selections.forEach { (key, value) ->
                if (key in keys) {
                    otherLevelSpellEntries.add(Triple(idx, key, value))
                }
            }
        }
        val otherLevelSpells = otherLevelSpellEntries
            .mapNotNull { (_, _, value) -> value as? ChoiceResult.Spells }

        val historicalSelections = baseSelections + otherLevelSpells
        val historicalChosen = historicalSelections.filterIsInstance<ChoiceResult.Spells>().flatMap { it.spellIndexes }
        val autoSpells = draft.baseInfo.staticSpells + draft.levelStack.flatMap { it.autoSpells }
        val historicalSpellSet = (historicalChosen + autoSpells).toSet()

        val currentChosen = currentLevelSpells.flatMap { it.spellIndexes }
        val duplicates = currentChosen.filter { it in historicalSpellSet }.distinct()
        return duplicates.isNotEmpty()
    }

    private suspend fun resolveActiveSpellKeysForLevel(
        draft: DraftCharacter,
        levelIndex: Int,
        levelStep: LevelStep,
        abilityModifier: Int
    ): Set<String> {
        val classLevel = draft.levelStack.take(levelIndex + 1).count { it.classIndex == levelStep.classIndex }
        val progressionData = getClassProgressionDataUseCase(
            draft = draft,
            classIndex = levelStep.classIndex,
            level = classLevel,
            subclassIndex = levelStep.subclassIndex,
            abilityModifier = abilityModifier,
            proficiencyProvider = { draft.getAllProficienciesWithLevels() },
            editingLevelIndex = levelIndex
        )

        val featuresToValidate = mutableListOf<Feature>()
        featuresToValidate.addAll(progressionData.partitionedFeatures.classSkillFeatures)
        featuresToValidate.addAll(progressionData.partitionedFeatures.inventoryChoiceFeatures)
        progressionData.aggregatedSpellFeature?.let { featuresToValidate.add(it) }

        return collectSpellChoiceKeys(featuresToValidate, SelectionSource.CLASS)
    }
    private fun collectSpellChoiceKeys(
        features: List<Feature>,
        source: SelectionSource
    ): Set<String> {
        val keys = mutableSetOf<String>()

        fun isPreparedKey(path: String): Boolean {
            return path.contains(DndConstants.VirtualKeys.PREPARED_SPELLS_PREFIX, ignoreCase = true)
        }

        fun walk(choice: FeatureChoiceDomain, currentKey: String) {
            when (choice) {
                is FeatureChoiceDomain.SelectSpell -> {
                    if (!isPreparedKey(currentKey)) {
                        keys.add(currentKey)
                    }
                }
                is FeatureChoiceDomain.SelectOption -> {
                    choice.options.forEach { opt ->
                        opt.subChoice?.let { sub ->
                            val nextKey = ChoicePathManager.append(currentKey, opt.id, 0)
                            walk(sub, nextKey)
                        }
                    }
                }
                else -> {}
            }
        }

        features.forEach { feature ->
            feature.choices.forEachIndexed { idx, choice ->
                val rootKey = ChoicePathManager.createIndexedKey(source, feature.index, idx)
                walk(choice, rootKey)
            }
        }

        return keys
    }

    private fun validateChoicesRecursively(
        features: List<Feature>,
        selections: Map<String, ChoiceResult>,
        issues: MutableList<ValidationIssue>,
        parentKey: String? = null,
        depth: Int = 0
    ) {
        if (depth > 10) return

        for (feature in features) {
            val effectiveSource = SelectionSource.CLASS

            feature.choices.forEachIndexed { cIdx, choice ->
                if (choice is FeatureChoiceDomain.InvalidChoice) {
                    issues.add(ValidationIssue(effectiveSource, "Ошибка: ${feature.name}", 1, 1))
                    return@forEachIndexed
                }

                val currentSelectionKey = if (parentKey == null) {
                    ChoicePathManager.createIndexedKey(effectiveSource, feature.index, cIdx)
                } else {
                    if (feature.choices.size > 1) {
                        ChoicePathManager.append(parentKey, "#$cIdx")
                    } else {
                        parentKey
                    }
                }

                val isTransparentContainer = (choice as? FeatureChoiceDomain.SelectOption)?.isTransparent == true

                if (isTransparentContainer && choice is FeatureChoiceDomain.SelectOption) {
                    choice.options.forEach { opt ->
                        opt.subChoice?.let { sub ->
                            val subFeature = Feature(
                                id = -1,
                                index = opt.id,
                                name = "${feature.name} (${opt.label})",
                                description = "",
                                choices = listOf(sub),
                                uiGroup = feature.uiGroup
                            )
                            validateChoicesRecursively(
                                features = listOf(subFeature),
                                selections = selections,
                                issues = issues,
                                parentKey = ChoicePathManager.append(currentSelectionKey, opt.id),
                                depth = depth + 1
                            )
                        }
                    }
                } else {
                    val selection = selections[currentSelectionKey]
                    val requiredCount = choice.count

                    val actualCount = when (selection) {
                        null, is ChoiceResult.Note -> 0
                        is ChoiceResult.Spells -> selection.spellIndexes.size
                        is ChoiceResult.SelectedOptions -> selection.items.size
                        is ChoiceResult.StatBonus -> selection.stats.size
                        is ChoiceResult.Skills -> selection.skillIndexes.size
                        else -> 0
                    }

                    if (actualCount < requiredCount) {
                        issues.add(ValidationIssue(effectiveSource, feature.name, requiredCount - actualCount, 1))
                    }

                    if (selection is ChoiceResult.SelectedOptions) {
                        selection.items.forEach { id ->
                            choice.options.find { it.id == id }?.subChoice?.let { sub ->
                                val subFeature = Feature(
                                    id = -1,
                                    index = id,
                                    name = id,
                                    description = "",
                                    choices = listOf(sub),
                                    uiGroup = feature.uiGroup
                                )
                                validateChoicesRecursively(
                                    features = listOf(subFeature),
                                    selections = selections,
                                    issues = issues,
                                    parentKey = ChoicePathManager.append(currentSelectionKey, id),
                                    depth = depth + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolveCurrentStats(draft: DraftCharacter): Map<String, Int> {
        return draft.baseInfo.baseAbilityScores.mapValues { (stat, value) ->
            value + (draft.baseInfo.aggregateStatBonuses[stat] ?: 0)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\level_up\ValidateLevelUpUseCase.kt
