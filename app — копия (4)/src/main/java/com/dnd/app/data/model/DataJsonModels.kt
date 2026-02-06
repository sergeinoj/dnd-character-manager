// Имя файла: app/src/main/java/com/dnd/app/data/model/DataJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.Serializable

/**
 * Описывает структуру элемента из массива granted_spell в JSON.
 */
@Serializable
data class GrantedSpellJson(
    val id: Int,
    val level: Int
)

/**
 * Описывает выбор навыков классом (skill_choices_json).
 */
@Serializable
data class ClassSkillsJson(
    val choices: List<String>
)

// FeatureChoiceJson и FeatureJsonModel УДАЛЕНЫ, так как перенесены в FeatureJsonModels.kt
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/model/DataJsonModels.kt