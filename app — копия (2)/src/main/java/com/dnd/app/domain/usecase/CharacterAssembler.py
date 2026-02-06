// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterAssembler @Inject constructor(
    private val repository: LibraryRepository,
    private val calculator: DndCalculator
) {
    /**
     * Собирает финальный объект CharacterDomain из черновика.
     * Реализация уровня Senior: использует предварительно рассчитанные бонусы из Draft,
     * обеспечивая принцип "What You See Is What You Get" (WYSIWYG).
     */
    suspend fun assemble(draft: DraftCharacter): CharacterDomain {
        val finalStatsMap = mutableMapOf<String, Int>()
        val skillProficiencies = mutableMapOf<String, Int>()
        val autoLearnedSpells = mutableListOf<String>()
        val features = mutableListOf<Feature>()

        var baseSpeed = 30
        var extraHp = 0

        // 1. БАЗОВЫЕ ХАРАКТЕРИСТИКИ + СТАТИЧЕСКИЕ БОНУСЫ (Раса + Подраса)
        // Мы берем значения, которые пользователь "натыкал" (8-15)
        // и прибавляем к ним staticRaceBonuses, которые ViewModel подготовила заранее.
        draft.baseInfo.baseAbilityScores.forEach { (stat, value) ->
            val staticBonus = draft.baseInfo.staticRaceBonuses[stat] ?: 0
            finalStatsMap[stat] = value + staticBonus
        }

        // 2. ПРИМЕНЕНИЕ ВЫБОРОВ ( raceSelections + levelStack.selections )
        // Здесь учитываются "плавающие" бонусы (например, +1 к любой стате у полуэльфа)
        val allSelections = mutableListOf<ChoiceResult>()
        allSelections.addAll(draft.baseInfo.raceSelections.values)
        draft.levelStack.forEach { allSelections.addAll(it.selections.values) }

        allSelections.forEach { result ->
            applyChoiceResult(result, finalStatsMap, skillProficiencies, autoLearnedSpells)
        }

        // 3. СБОРКА СПОСОБНОСТЕЙ И ЛОГИКИ ПРАВИЛ
        // Нам все еще нужны объекты фич для определения спец-логики (типа скорости или доп. ХП)
        val race = repository.getAllParentRaces().find { it.index == draft.baseInfo.raceIndex }
        if (race != null) {
            baseSpeed = race.speed
            val raceFeatures = repository.getRaceFeatures(race.id, draft.baseInfo.subraceIndex)
            features.addAll(raceFeatures)

            raceFeatures.forEach { feat ->
                if (feat.changeRule) {
                    when(feat.index) {
                        "fleet-of-foot" -> baseSpeed = 35
                        "dwarven-toughness" -> extraHp += draft.levelStack.size.coerceAtLeast(1)
                    }
                }
            }
        }

        // 4. КЛАССОВАЯ ПРОГРЕССИЯ
        val conModInitial = calculator.calculateModifier(finalStatsMap["CON"] ?: 10)
        var hpMax = extraHp

        draft.levelStack.forEachIndexed { index, step ->
            val lvl = index + 1
            val classInfo = repository.getAllClasses().find { it.index == step.classIndex }

            // Расчет ХП
            if (index == 0) hpMax += (classInfo?.hitDie ?: 8) + conModInitial
            else hpMax += step.hpIncrease + conModInitial

            // Фичи класса
            features.addAll(repository.getProgressionFeatures(step.classIndex, lvl, step.subclassIndex))
        }

        val classString = draft.levelStack.groupBy { it.classIndex }
            .map { (idx, list) -> "${idx.replaceFirstChar { it.uppercase() }} ${list.size}" }
            .joinToString(" / ")

        return CharacterDomain(
            id = draft.id,
            name = draft.name.ifBlank { "Герой" },
            raceName = race?.name ?: "",
            className = classString,
            level = draft.levelStack.size.coerceAtLeast(1),
            stats = Stats(
                strength = finalStatsMap["STR"] ?: 10,
                dexterity = finalStatsMap["DEX"] ?: 10,
                constitution = finalStatsMap["CON"] ?: 10,
                intelligence = finalStatsMap["INT"] ?: 10,
                wisdom = finalStatsMap["WIS"] ?: 10,
                charisma = finalStatsMap["CHA"] ?: 10
            ),
            hpMax = hpMax, hpCurrent = hpMax,
            speed = baseSpeed,
            features = features,
            raceSpellIds = autoLearnedSpells.distinct(),
            skillProficiencies = skillProficiencies,
            bio = Bio(
                alignment = draft.baseInfo.alignmentIndex,
                traits = draft.baseInfo.personalityTrait,
                ideals = draft.baseInfo.ideal,
                bonds = draft.baseInfo.bond,
                flaws = draft.baseInfo.flaw
            )
        )
    }

    private fun applyChoiceResult(
        result: ChoiceResult,
        stats: MutableMap<String, Int>,
        skills: MutableMap<String, Int>,
        spells: MutableList<String>
    ) {
        when (result) {
            is ChoiceResult.StatBonus -> result.bonuses.forEach { (s, b) ->
                val key = s.take(3).uppercase()
                stats[key] = (stats[key] ?: 0) + b
            }
            is ChoiceResult.Skills -> result.skillIndexes.forEach { skills[it] = 1 }
            is ChoiceResult.Spells -> spells.addAll(result.spellIndexes)
            is ChoiceResult.SelectedOptions -> {
                result.items.forEach { if (it.contains("skill-")) skills[it] = 1 }
            }
            else -> {}
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt