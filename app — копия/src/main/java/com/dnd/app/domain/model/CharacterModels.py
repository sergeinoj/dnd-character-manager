// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterDomain(
    val id: Long = 0,
    val name: String = "",
    val raceId: Int = 0,
    val classId: Int = 0,
    val level: Int = 1,
    val stats: Stats = Stats(),
    val hpCurrent: Int = 10,
    val hpMax: Int = 10,
    val inventoryIds: List<Int> = emptyList(),
    val spellsKnownIds: List<Int> = emptyList(),
    val bio: Bio = Bio(),
    val skillProficiencies: Map<String, Int> = emptyMap()
)

@Serializable
data class Stats(
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val copper: Int = 0,
    val silver: Int = 0,
    val gold: Int = 0
)

@Serializable
data class Bio(
    val traits: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val background: String = "",
    val notes: String = ""
)

data class Spell(
    val id: Int,
    val name: String,
    val level: Int,
    val school: String,
    val castingTime: String,
    val range: String,
    val components: String,
    val duration: String,
    val description: String,
    val isConcentration: Boolean,
    val isRitual: Boolean
)

data class Weapon(
    val id: Int,
    val name: String,
    val damage: String,
    val damageType: String,
    val cost: String,
    val weight: String,
    val properties: String
)

data class Feature(
    val id: Int,
    val name: String,
    val type: String,
    val description: String,
    val modifiers: String
)

data class Race(
    val id: Int,
    val name: String,
    val statBonuses: Map<String, Int>
)

data class ClassInfo(
    val id: Int,
    val name: String,
    val hitDie: Int
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt