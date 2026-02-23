// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\SpellChoiceAggregatorUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.*
import com.dnd.app.util.DndLocalization
import javax.inject.Inject
import javax.inject.Singleton

data class AggregationResult(
    val aggregatedFeature: Feature?,
    val consumedFeatureIndexes: Set<String>,
    val modifiedFeatures: Map<String, Feature> = emptyMap()
)


@Singleton
class SpellChoiceAggregatorUseCase @Inject constructor() {

    suspend operator fun invoke(
        features: List<Feature>,
        excludedSpells: Set<String>,
        currentlySelectedIds: Set<String> = emptySet(),
        maxSpellLevel: Int = Int.MAX_VALUE
    ): AggregationResult {
        if (features.isEmpty()) return AggregationResult(null, emptySet())

        val consumedFeatureIndexes = mutableSetOf<String>()
        val modifiedFeatures = mutableMapOf<String, Feature>()
        val allStaticSpells = mutableListOf<Spell>()
        val poolGroups = mutableMapOf<String, MutableList<FeatureChoiceDomain.SelectSpell>>()

        features.forEach { feature ->
            val classKey = feature.classIndex ?: feature.subclassIndex ?: DndConstants.DEFAULT_CLASS_INDEX
            val allMagicChoices = feature.choices.flatMap { recursiveFindMagic(it) }
            val aggregatable = allMagicChoices.filter { isAggregatable(it.poolType) }

            if (aggregatable.isEmpty() && feature.embeddedSpells.isEmpty()) {
                return@forEach
            }

            if (feature.embeddedSpells.isNotEmpty()) {
                allStaticSpells.addAll(feature.embeddedSpells)
            }

            aggregatable.forEach { choice ->

                val compositeId = "${classKey}_${choice.poolType}"
                poolGroups.getOrPut(compositeId) { mutableListOf() }.add(choice)
            }

            val cleanChoices = feature.choices.mapNotNull { recursiveSanitize(it, aggregatable) }
            val sanitizedFeature = feature.copy(embeddedSpells = emptyList(), choices = cleanChoices)

            val isZombie = sanitizedFeature.description.isBlank() &&
                    cleanChoices.isEmpty() &&
                    feature.referenceJson.isNullOrBlank() &&
                    feature.index != DndConstants.VirtualKeys.AGGREGATED_SPELL_CHOICE

            if (isZombie) consumedFeatureIndexes.add(feature.index)
            else modifiedFeatures[feature.index] = sanitizedFeature
        }

        if (allStaticSpells.isEmpty() && poolGroups.isEmpty()) {
            return AggregationResult(null, emptySet(), modifiedFeatures)
        }

        val containerOptions = mutableListOf<ChoiceOption>()

        poolGroups.forEach { (compositeId, choices) ->
            val totalCount = choices.sumOf { it.count }
            val allOptions = choices.flatMap { it.options }
            .filterNot { opt -> opt.id in excludedSpells && opt.id !in currentlySelectedIds }
                .filter { opt ->
                    val lvl = opt.spell?.level ?: 0
                    lvl <= maxSpellLevel
                }
                .distinctBy { it.id }
                .sortedBy { it.label }

            if (allOptions.isNotEmpty()) {
                val classIdx = compositeId.substringBefore("_")
                val originalPoolType = compositeId.substringAfter("_")



                containerOptions.add(
                    ChoiceOption(
                        id = originalPoolType,
                        label = DndLocalization.getSpellPoolName(originalPoolType, classIdx),
                        subChoice = FeatureChoiceDomain.SelectSpell(
                            count = totalCount,
                            poolType = originalPoolType,
                            autoAdjustLimit = choices.any { it.autoAdjustLimit },
                            options = allOptions
                        )
                    )
                )
            }
        }

        val aggregatedFeature = Feature(
            id = -999,
            index = DndConstants.VirtualKeys.AGGREGATED_SPELL_CHOICE,
            name = "Магия",
            description = "",
            embeddedSpells = allStaticSpells.distinctBy { it.index }.sortedBy { it.level },
            choices = if (containerOptions.isNotEmpty()) {
                listOf(FeatureChoiceDomain.SelectOption(1, containerOptions.sortedBy { it.label }, isTransparent = true))
            } else emptyList(),
            uiGroup = "SPELLS"
        )

        return AggregationResult(aggregatedFeature, consumedFeatureIndexes, modifiedFeatures)
    }

    private fun isAggregatable(poolType: String): Boolean {
        if (poolType.startsWith("virtual-")) return true
        val t = poolType.lowercase()
        return t.contains("class_") || t.contains("spells_") || t.contains("cantrips_") || t.contains("_0")
    }

    private fun recursiveFindMagic(choice: FeatureChoiceDomain): List<FeatureChoiceDomain.SelectSpell> {
        return when (choice) {
            is FeatureChoiceDomain.SelectSpell -> listOf(choice)
            is FeatureChoiceDomain.SelectOption -> choice.options.flatMap { opt -> opt.subChoice?.let { recursiveFindMagic(it) } ?: emptyList() }
            else -> emptyList()
        }
    }

    private fun recursiveSanitize(choice: FeatureChoiceDomain, consumed: List<FeatureChoiceDomain.SelectSpell>): FeatureChoiceDomain? {
        return when (choice) {
            is FeatureChoiceDomain.SelectSpell -> if (choice in consumed) null else choice
            is FeatureChoiceDomain.SelectOption -> {
                val sanitized = choice.options.mapNotNull { opt -> opt.copy(subChoice = opt.subChoice?.let { recursiveSanitize(it, consumed) }) }
                if (sanitized.isEmpty() && choice.options.any { it.subChoice != null }) null
                else choice.copy(options = sanitized)
            }
            else -> choice
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\SpellChoiceAggregatorUseCase.kt
