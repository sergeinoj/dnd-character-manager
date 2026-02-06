// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterDomain(
    val id: Long = 0,
    val name: String = "",
    val raceName: String = "",
    val className: String = "",
    val level: Int = 1,
    val stats: Stats = Stats(),
    val hpCurrent: Int = 10,
    val hpMax: Int = 10,
    val speed: Int = 30,
    val inventoryIds: List<Int> = emptyList(),
    val spellsKnownIds: List<Int> = emptyList(),
    val raceSpellIds: List<String> = emptyList(),
    val features: List<Feature> = emptyList(),
    val bio: Bio = Bio(),
    val skillProficiencies: Map<String, Int> = emptyMap()
)

@Serializable
data class Stats(
    val strength: Int = 10, val dexterity: Int = 10, val constitution: Int = 10,
    val intelligence: Int = 10, val wisdom: Int = 10, val charisma: Int = 10,
    val copper: Int = 0, val silver: Int = 0, val gold: Int = 0
)

@Serializable
data class Bio(
    val alignment: String = "",
    val background: String = "",
    val backgroundName: String = "",
    val traits: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val notes: String = ""
)

@Serializable
data class Race(
    val id: Int,
    val index: String,
    val name: String,
    val description: String?,
    val speed: Int,
    val baseStats: Map<String, Int> = emptyMap(),
    val baseProficiencies: List<String> = emptyList(),
    val raceChoices: List<FeatureChoiceDomain> = emptyList()
)

@Serializable
data class ClassInfo(
    val id: Int,
    val index: String,
    val name: String,
    val hitDie: Int,
    val subclasses: List<SubclassInfo> = emptyList()
)

@Serializable
data class SubclassInfo(
    val index: String,
    val name: String,
    val flavor: String,
    val description: String
)

@Serializable
data class Background(
    val id: Int,
    val name: String,
    val features: List<Feature>,
    // [ИЗМЕНЕНО v1.28] Поле `staticEquipment` переименовано для ясности.
    val equipment: List<String> = emptyList(),
    val startingGold: Int = 0,
    val featureIndices: List<String> = emptyList(),
    val personalityTraits: List<String> = emptyList(),
    val ideals: List<String> = emptyList(),
    val bonds: List<String> = emptyList(),
    val flaws: List<String> = emptyList()
)

@Serializable
data class Spell(
    val id: Int, val index: String, val name: String, val level: Int,
    val school: String,
    val castingTime: String,
    val range: String,
    val components: String, val duration: String, val description: String,
    val isConcentration: Boolean, val isRitual: Boolean
)

@Serializable
data class Weapon(
    val id: Int, val name: String, val damage: String,
    val damageType: String,
    val cost: String, val weight: String, val properties: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt