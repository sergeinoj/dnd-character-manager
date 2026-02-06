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
    @Serializable
    data class RuleEffect(val effectType: String, val value: String) : ChoiceResult()
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
    val aggregateStatBonuses: Map<String, Int> = emptyMap(),
    val raceSelections: Map<String, ChoiceResult> = emptyMap(),
    val backgroundSelections: Map<String, ChoiceResult> = emptyMap(),
    // [ИЗМЕНЕНО v1.25] Добавлен выбор инвентаря.
    val inventorySelections: Map<String, ChoiceResult> = emptyMap(),
    val staticProficiencies: List<String> = emptyList(),
    // [НОВЫЙ v1.25] Для хранения гарантированного снаряжения от класса/предыстории
    val staticEquipment: List<String> = emptyList()
)

@Serializable
data class DraftCharacter(
    val id: Long = 0,
    val name: String = "",
    val baseInfo: BaseInfo = BaseInfo(),
    val levelStack: List<LevelStep> = emptyList()
) {
    /**
     * [НОВЫЙ ПРИВАТНЫЙ МЕТОД]
     * Возвращает только статичные владения (навыки, инструменты), полученные от расы/класса.
     * Это чистый "черный список" для дедупликации владений.
     */
    private fun getStaticProficiencyExclusions(): Set<String> {
        return baseInfo.staticProficiencies.toSet()
    }

    /**
     * [ИЗМЕНЕНО v1.25] Возвращает ВСЕ исключения для ВЛАДЕНИЙ (статические + динамические).
     * Включает навыки и ИНСТРУМЕНТЫ.
     * Используется для UI, чтобы скрыть уже выбранные опции в дропдаунах.
     * НЕ содержит ключи характеристик.
     */
    fun getProficiencyExclusions(): Set<String> {
        val exclusions = getStaticProficiencyExclusions().toMutableSet()
        // 3. Все сделанные пользователем выборы (кроме бонусов характеристик)
        baseInfo.raceSelections.values.forEach { exclusions.addAll(extractIds(it)) }
        baseInfo.backgroundSelections.values.forEach { exclusions.addAll(extractIds(it)) }

        levelStack.forEach { step ->
            step.selections.forEach { (featIdx, res) ->
                if (!featIdx.contains("expertise")) {
                    exclusions.addAll(extractIds(res))
                }
            }
        }
        return exclusions
    }

    /**
     * Возвращает "жесткие" исключения - статические бонусы и владения, которые нельзя изменить.
     * Используется для валидации и сброса пользовательских выборов.
     */
    fun getHardExclusions(): Set<String> {
        val exclusions = mutableSetOf<String>()
        // 1. Статичные бонусы характеристик (чтобы не выбирать их в динамических)
        baseInfo.aggregateStatBonuses.keys.forEach { exclusions.add(it) }
        // 2. Статичные владения (навыки, инструменты)
        exclusions.addAll(baseInfo.staticProficiencies)
        return exclusions
    }

    fun getPickedSkills(): List<String> {
        val skills = mutableSetOf<String>()
        skills.addAll(baseInfo.staticProficiencies.filter { it.startsWith("skill-") })
        baseInfo.raceSelections.values.filterIsInstance<ChoiceResult.Skills>().forEach { skills.addAll(it.skillIndexes) }
        levelStack.forEach { step ->
            step.selections.values.filterIsInstance<ChoiceResult.Skills>().forEach { skills.addAll(it.skillIndexes) }
            step.selections.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { opt ->
                skills.addAll(opt.items.filter { it.startsWith("skill-") })
            }
        }
        return skills.toList()
    }

    /**
     * [ИЗМЕНЕНО v1.25] Вспомогательный метод, который теперь корректно игнорирует бонусы характеристик
     * и включает инструменты.
     */
    private fun extractIds(result: ChoiceResult): List<String> {
        return when (result) {
            is ChoiceResult.Skills -> result.skillIndexes
            is ChoiceResult.Spells -> result.spellIndexes
            is ChoiceResult.SelectedOptions -> result.items.filter {
                it.startsWith("skill-") || it.startsWith("tool-")
            }
            // Ключи характеристик (STR, DEX) НЕ должны попадать в список исключений для владений.
            is ChoiceResult.StatBonus -> emptyList()
            else -> emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt