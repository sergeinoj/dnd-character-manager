// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\GetClassProgressionDataUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoicePathManager
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.PartitionedFeatures
import com.dnd.app.domain.model.SelectionSource
import com.dnd.app.domain.usecase.class_feature_orchestration.GetFeaturesForLevelUseCase
import com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails
import javax.inject.Inject
import javax.inject.Singleton

data class ClassProgressionData(
    val partitionedFeatures: PartitionedFeatures,
    val aggregatedSpellFeature: Feature?,
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails>,
    val consumedFeatureIndexes: Set<String> = emptySet()
)


@Singleton
class GetClassProgressionDataUseCase @Inject constructor(
    private val getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase,
    private val partitionUseCase: PartitionClassFeaturesUseCase,
    private val spellAggregatorUseCase: SpellChoiceAggregatorUseCase,
    private val unpackEquipmentUseCase: UnpackEquipmentUseCase,
    private val calculator: com.dnd.app.domain.calculator.DndCalculator
) {
    suspend operator fun invoke(
        draft: DraftCharacter,
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int,
        additionalIndexes: List<String> = emptyList(),
        proficiencyProvider: (() -> Map<String, Int>)? = null,
        editingLevelIndex: Int
    ): ClassProgressionData {


        val isGenesis = editingLevelIndex == 0

        val featuresResult = getFeaturesForLevelUseCase.invokeWithContext(
            classIndex = classIndex,
            level = level,
            subclassIndex = subclassIndex,
            abilityModifier = abilityModifier,
            isGenesis = isGenesis,
            proficiencyProvider = proficiencyProvider
        )

        val partitioned = partitionUseCase(featuresResult)


        val maxSpellLevel = calculator.getMaxSpellLevel(classIndex, level)
        val safeLevelIndex = editingLevelIndex.coerceAtLeast(0).coerceAtMost(draft.levelStack.size)
        val learnedSpellExclusions = draft.getHistoricalLearnedSpells(safeLevelIndex)
        val currentSelections = draft.levelStack.getOrNull(editingLevelIndex)?.selections ?: emptyMap()
        val aggregatedSelectionKey = ChoicePathManager.createRootKey(
            SelectionSource.CLASS,
            DndConstants.VirtualKeys.AGGREGATED_SPELL_CHOICE
        )
        val currentlySelectedIds = currentSelections.entries
            .asSequence()
            .filter { (key, value) ->
                value is ChoiceResult.Spells &&
                        (key == aggregatedSelectionKey || ChoicePathManager.isChildOf(aggregatedSelectionKey, key))
            }
            .flatMap { (_, value) -> (value as ChoiceResult.Spells).spellIndexes.asSequence() }
            .toSet()

        val aggregationResult = spellAggregatorUseCase(
            features = partitioned.classSkillFeatures,
            excludedSpells = learnedSpellExclusions,
            currentlySelectedIds = currentlySelectedIds,
            maxSpellLevel = maxSpellLevel
        )

        val aggregatedSpellFeature = aggregationResult.aggregatedFeature
        val consumedIndexes = aggregationResult.consumedFeatureIndexes
        val modifiedMap = aggregationResult.modifiedFeatures


        val finalClassSkillFeatures = partitioned.classSkillFeatures
            .filter { it.index !in consumedIndexes }
            .map { feature -> modifiedMap[feature.index] ?: feature }

        val finalPartitionedFeatures = partitioned.copy(classSkillFeatures = finalClassSkillFeatures)


        val allPossibleOptionIds = mutableSetOf<String>()
        fun collectIds(choice: FeatureChoiceDomain?, depth: Int = 0) {
            if (choice == null || depth > 5) return
            choice.options.forEach { option ->
                allPossibleOptionIds.add(option.id)
                collectIds(option.subChoice, depth + 1)
            }
        }
        partitioned.inventoryChoiceFeatures.forEach { feature ->
            feature.choices.forEach { choice -> collectIds(choice) }
        }

        val initialIndexes = (allPossibleOptionIds.toList() + additionalIndexes).distinct()
        val unpackedEquipment = unpackEquipmentUseCase(initialIndexes)

        return ClassProgressionData(
            partitionedFeatures = finalPartitionedFeatures,
            aggregatedSpellFeature = aggregatedSpellFeature,
            unpackedEquipmentOptions = unpackedEquipment,
            consumedFeatureIndexes = consumedIndexes
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\GetClassProgressionDataUseCase.kt
