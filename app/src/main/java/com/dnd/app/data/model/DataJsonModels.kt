// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\model\DataJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
data class GrantedSpellJson(
    val id: Int,
    val level: Int
)


@Serializable
data class ClassSkillsJson(
    val choices: List<String>
)


@Serializable
data class MultiClassingJson(
    val proficiencies: List<ReferenceJson> = emptyList(),
    @SerialName("proficiency_choices") val proficiencyChoices: List<JsonObject> = emptyList(),

    @SerialName("feature_indices") val featureIndices: List<String> = emptyList(),
    val prerequisites: List<MulticlassPrerequisiteJson> = emptyList()
)

@Serializable
data class MulticlassPrerequisiteJson(
    @SerialName("ability_score") val abilityScore: ReferenceJson? = null,
    @SerialName("minimum_score") val minimumScore: Int = 0
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\model\DataJsonModels.kt
