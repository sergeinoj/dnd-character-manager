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
    val inventorySelections: Map<String, ChoiceResult> = emptyMap(),
    val staticProficiencies: List<String> = emptyList(),
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
     * [ОБНОВЛЕНО v1.31] Возвращает ВСЕ выбранные идентификаторы (навыки, владения, заклинания, черты).
     * Является единым источником правды о том, что у персонажа уже "есть".
     */
    fun getAllPickedIds(): Set<String> {
        val allIds = mutableSetOf<String>()
        allIds.addAll(baseInfo.staticProficiencies)

        val allSelectionMaps = listOf(
            baseInfo.raceSelections,
            baseInfo.backgroundSelections,
            baseInfo.inventorySelections
        ) + levelStack.map { it.selections }

        for (selectionMap in allSelectionMaps) {
            selectionMap.values.forEach { result ->
                when (result) {
                    is ChoiceResult.Skills -> allIds.addAll(result.skillIndexes)
                    is ChoiceResult.Spells -> allIds.addAll(result.spellIndexes)
                    is ChoiceResult.SelectedOptions -> allIds.addAll(result.items)
                    else -> {} // StatBonus, Note, RuleEffect не добавляются как "выбранные" id
                }
            }
        }
        return allIds
    }

    /**
     * [ОБНОВЛЕНО v1.31] Использует `getAllPickedIds` для получения всех навыков, которыми владеет персонаж.
     * Используется UI для корректного отображения списка экспертизы.
     */
    fun getPickedSkills(): List<String> {
        return getAllPickedIds().filter { it.startsWith("skill-") }.toList()
    }

    /**
     * [ОБНОВЛЕНО v1.31] Возвращает ВСЕ исключения для ВЛАДЕНИЙ (статические + динамические).
     * Используется для UI, чтобы скрыть уже выбранные опции в дропдаунах.
     * Корректно игнорирует выборы для экспертизы.
     */
    fun getProficiencyExclusions(): Set<String> {
        // Начинаем со всех ID, которые являются владениями
        val exclusions = getAllPickedIds().filterNot {
            it.startsWith("spell-") || it.startsWith("feat-") // Исключаем заклинания и черты
        }.toMutableSet()

        // Специальная логика для экспертизы: мы не хотим, чтобы выбор экспертизы
        // исключал сам себя из других списков экспертизы.
        val expertiseSelections = mutableSetOf<String>()
        val allSelectionMaps = listOf(
            baseInfo.raceSelections,
            baseInfo.backgroundSelections
        ) + levelStack.map { it.selections }

        allSelectionMaps.forEach { selectionMap ->
            selectionMap.forEach { (key, result) ->
                if (key.contains("expertise", ignoreCase = true)) {
                    when (result) {
                        is ChoiceResult.Skills -> expertiseSelections.addAll(result.skillIndexes)
                        is ChoiceResult.SelectedOptions -> expertiseSelections.addAll(result.items)
                        else -> {}
                    }
                }
            }
        }
        // Удаляем выборы экспертизы из общего списка исключений
        exclusions.removeAll(expertiseSelections)
        return exclusions
    }

    /**
     * [ВОССТАНОВЛЕН v1.31.1] Возвращает "жесткие" исключения - статические бонусы и владения, которые нельзя изменить.
     * Используется для валидации и сброса пользовательских выборов в BakeCharacterUseCase.
     */
    fun getHardExclusions(): Set<String> {
        val exclusions = mutableSetOf<String>()
        // 1. Статичные бонусы характеристик (чтобы не выбирать их в динамических)
        baseInfo.aggregateStatBonuses.keys.forEach { exclusions.add(it) }
        // 2. Статичные владения (навыки, инструменты)
        exclusions.addAll(baseInfo.staticProficiencies)
        return exclusions
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt