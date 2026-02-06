// Имя файла: data/model/WizardJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Описывает структуру raw_data из таблицы features.
 * Пример: {"type": 5, "chooseCount": 2, "common": ["Атлетика", ...]}
 */
@Serializable
data class FeatureRawData(
    val type: Int = 0, // 0=Passive, 5=Choice, etc.
    val chooseCount: Int = 0,
    val common: List<String> = emptyList(), // Варианты выбора
    val attributes: List<String> = emptyList(), // Затрагиваемые статы (для бонусов)
    val value: Int = 0 // Числовое значение (например, +1 к КД)
)

/**
 * Описывает структуру saving_throws_json и skill_choices_json из таблицы classes
 */
@Serializable
data class ClassJsonData(
    val savingThrows: List<String> = emptyList(),
    val skillChoices: List<String> = emptyList() // ["Атлетика", "Внимательность"]
)

/**
 * Описывает stats_json из таблицы races
 */
@Serializable
data class RaceStatsJson(
    val strength: Int = 0,
    val dexterity: Int = 0,
    val constitution: Int = 0,
    val intelligence: Int = 0,
    val wisdom: Int = 0,
    val charisma: Int = 0
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: data/model/WizardJsonModels.kt