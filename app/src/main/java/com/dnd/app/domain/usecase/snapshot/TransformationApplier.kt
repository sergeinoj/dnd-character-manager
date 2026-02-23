package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.snapshot.SkillModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransformationApplier @Inject constructor(
    private val calculator: DndCalculator
) {

    data class Configuration(
        val stats: Map<String, Int> = emptyMap(),
        val skillBonuses: Map<String, Int> = emptyMap()
    )

    data class Result(
        val statRegistry: StatRegistry,
        val skills: List<SkillModel>
    )

    fun apply(
        statRegistry: StatRegistry,
        skills: List<SkillModel>,
        configuration: Configuration
    ): Result {
        if (configuration.stats.isEmpty() && configuration.skillBonuses.isEmpty()) {
            return Result(statRegistry, skills)
        }

        val overrides = configuration.stats
            .mapKeys { it.key.uppercase() }
            .filterKeys { it in abilityKeys }

        val (transformedRegistry, abilityDeltas) = buildTransformedRegistry(statRegistry, overrides)

        val adaptedSkills = skills.map { skill ->
            val delta = abilityDeltas[skill.statCode.uppercase()] ?: 0
            val baseValue = parseModifier(skill.modifier) + delta
            val beastValue = configuration.skillBonuses[skill.code]
            val mergedValue = beastValue?.let { maxOf(baseValue, it) } ?: baseValue
            skill.copy(modifier = calculator.formatModifier(mergedValue))
        }

        return Result(transformedRegistry, adaptedSkills)
    }

    private fun buildTransformedRegistry(
        registry: StatRegistry,
        overrides: Map<String, Int>
    ): Pair<StatRegistry, Map<String, Int>> {
        if (overrides.isEmpty()) return registry to emptyMap()

        val scores = registry.scores.toMutableMap()
        val modifiers = registry.modifiers.toMutableMap()
        val models = registry.models.toMutableMap()
        val deltas = mutableMapOf<String, Int>()

        overrides.forEach { (code, value) ->
            val currentModel = registry.models[code] ?: return@forEach
            val oldModifier = parseModifier(currentModel.modifier)
            val newModifier = calculator.calculateModifier(value)
            val saveContribution = parseModifier(currentModel.saveModifier) - oldModifier

            scores[code] = value
            modifiers[code] = newModifier
            models[code] = currentModel.copy(
                value = value,
                modifier = calculator.formatModifier(newModifier),
                saveModifier = calculator.formatModifier(newModifier + saveContribution)
            )

            deltas[code] = newModifier - oldModifier
        }

        return StatRegistry(scores, modifiers, models) to deltas
    }

    private fun parseModifier(input: String): Int {
        val normalized = input.replace("+", "").replace("−", "-")
        return normalized.toIntOrNull() ?: 0
    }

    companion object {
        private val abilityKeys = setOf("STR", "DEX", "CON")
    }
}
