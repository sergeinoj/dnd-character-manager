// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/SpellChoiceAggregatorUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Преобразует плоский список способностей (features) с выбором заклинаний в одну "виртуальную"
 * способность с иерархическим, сгруппированным выбором, готовым для UI.
 * Это позволяет унифицировать отображение выбора классовых заклинаний и заклинаний из черт.
 */
@Singleton
class SpellChoiceAggregatorUseCase @Inject constructor() {
    suspend operator fun invoke(features: List<Feature>): Feature? {
        if (features.isEmpty()) return null

        val cantripChoices = mutableListOf<FeatureChoiceDomain.SelectSpell>()
        val leveledSpellChoices = mutableListOf<FeatureChoiceDomain.SelectSpell>()

        // 1. Разделяем все выборы на заговоры и уровневые заклинания
        features.forEach { feature ->
            feature.choices.forEach { choice ->
                if (choice is FeatureChoiceDomain.SelectSpell) {
                    val isCantrip = choice.options.firstOrNull()?.spell?.level == 0
                    if (isCantrip) {
                        cantripChoices.add(choice)
                    } else {
                        leveledSpellChoices.add(choice)
                    }
                }
            }
        }

        if (cantripChoices.isEmpty() && leveledSpellChoices.isEmpty()) return null

        val containerOptions = mutableListOf<ChoiceOption>()

        // 2. Агрегируем выборы заговоров в одну группу
        if (cantripChoices.isNotEmpty()) {
            val totalCount = cantripChoices.sumOf { it.count }
            val allOptions = cantripChoices.flatMap { it.options }.distinctBy { it.id }.sortedBy { it.label }
            val aggregatedCantripChoice = FeatureChoiceDomain.SelectSpell(totalCount, "cantrips_aggregated", allOptions)
            containerOptions.add(
                ChoiceOption(
                    id = "cantrips",
                    label = "Заговоры", // ИЗМЕНЕНО
                    subChoice = aggregatedCantripChoice
                )
            )
        }

        // 3. Агрегируем выборы уровневых заклинаний в одну группу
        if (leveledSpellChoices.isNotEmpty()) {
            val totalCount = leveledSpellChoices.sumOf { it.count }
            val allOptions = leveledSpellChoices.flatMap { it.options }.distinctBy { it.id }.sortedBy { it.label }
            val aggregatedLeveledChoice = FeatureChoiceDomain.SelectSpell(totalCount, "leveled_aggregated", allOptions)
            containerOptions.add(
                ChoiceOption(
                    id = "level_1_spells",
                    label = "Заклинания 1 уровня", // ИЗМЕНЕНО
                    subChoice = aggregatedLeveledChoice
                )
            )
        }

        // 4. Создаем виртуальную родительскую способность-контейнер
        return Feature(
            id = -999,
            index = "aggregated-spell-choice",
            name = "Магия",
            description = "",
            choices = listOf(
                FeatureChoiceDomain.SelectOption(
                    count = 1,
                    options = containerOptions,
                    description = "@CONTAINER@"
                )
            ),
            uiGroup = "SPELLS"
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/SpellChoiceAggregatorUseCase.kt