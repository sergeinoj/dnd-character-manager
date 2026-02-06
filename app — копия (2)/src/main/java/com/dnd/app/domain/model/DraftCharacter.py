// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ChoiceResult {
    @Serializable
    data class Skills(val skillIndexes: List<String>) : ChoiceResult()
    @Serializable
    data class Spells(val spellIndexes: List<String>) : ChoiceResult()
    @Serializable
    data class SelectedOptions(val items: List<String>) : ChoiceResult()
    @Serializable
    data class StatBonus(val bonuses: Map<String, Int>) : ChoiceResult()
    @Serializable
    data class Note(val text: String) : ChoiceResult()
}

@Serializable
data class LevelStep(
    val classIndex: String,
    val subclassIndex: String? = null,
    val hpIncrease: Int = 0,
    val selections: Map<String, ChoiceResult> = emptyMap()
)

@Serializable
data class BaseInfo(
    val raceIndex: String = "",
    val subraceIndex: String? = null,
    val backgroundIndex: String = "",
    val alignmentIndex: String = "",
    val personalityTrait: String = "",
    val ideal: String = "",
    val bond: String = "",
    val flaw: String = "",
    val baseAbilityScores: Map<String, Int> = mapOf(
        "STR" to 8, "DEX" to 8, "CON" to 8,
        "INT" to 8, "WIS" to 8, "CHA" to 8
    ),
    val staticRaceBonuses: Map<String, Int> = emptyMap(),
    val raceSelections: Map<String, ChoiceResult> = emptyMap(),
    val backgroundSelections: Map<String, ChoiceResult> = emptyMap()
)

@Serializable
data class DraftCharacter(
    val id: Long = 0,
    val name: String = "",
    val baseInfo: BaseInfo = BaseInfo(),
    val levelStack: List<LevelStep> = emptyList()
) {
    /**
     * Собирает все ID навыков, выбранных игроком на данный момент.
     * Используется для наполнения списка Экспертизы.
     */
    fun getPickedSkills(): List<String> {
        val skills = mutableSetOf<String>()

        // 1. Из расы
        baseInfo.raceSelections.values.filterIsInstance<ChoiceResult.Skills>().forEach {
            skills.addAll(it.skillIndexes)
        }

        // 2. Из классов (проходим по всем уровням)
        levelStack.forEach { step ->
            step.selections.values.filterIsInstance<ChoiceResult.Skills>().forEach {
                skills.addAll(it.skillIndexes)
            }
            // Инструменты или навыки из SelectedOptions (иногда навыки приходят там)
            step.selections.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { opt ->
                skills.addAll(opt.items.filter { it.startsWith("skill-") })
            }
        }

        return skills.toList()
    }

    fun getGlobalExclusions(): Set<String> {
        val exclusions = mutableSetOf<String>()
        baseInfo.staticRaceBonuses.keys.forEach { exclusions.add(it) }
        baseInfo.raceSelections.values.forEach { exclusions.addAll(extractIds(it)) }
        levelStack.forEach { step ->
            // ВАЖНО: Мы НЕ добавляем выборы экспертизы в исключения,
            // иначе один дропдаун экспертизы заблокирует другой.
            step.selections.forEach { (featIdx, res) ->
                if (!featIdx.contains("expertise")) {
                    exclusions.addAll(extractIds(res))
                }
            }
        }
        return exclusions
    }

    private fun extractIds(result: ChoiceResult): List<String> {
        return when (result) {
            is ChoiceResult.Skills -> result.skillIndexes
            is ChoiceResult.Spells -> result.spellIndexes
            is ChoiceResult.SelectedOptions -> result.items
            is ChoiceResult.StatBonus -> result.bonuses.keys.toList()
            else -> emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt