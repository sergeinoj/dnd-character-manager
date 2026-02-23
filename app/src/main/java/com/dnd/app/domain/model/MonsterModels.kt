// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\MonsterModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import com.dnd.app.domain.model.monster.EffectTrigger
import kotlinx.serialization.Serializable
@Serializable
data class MonsterRecord(
    val index: String,
    val name: String,
    val description: String? = null,
    val size: String? = null,
    val type: String? = null,
    val alignment: String? = null,
    val armorClass: Int? = null,
    val hitPoints: Int? = null,
    val speed: Map<String, String> = emptyMap(),
    val stats: Map<String, Int> = emptyMap(),
    val challengeRating: Double? = null,
    val senses: Map<String, String> = emptyMap(),
    val languages: List<String> = emptyList(),
    val damageResistances: List<String> = emptyList(),
    val damageImmunities: List<String> = emptyList(),
    val conditionImmunities: List<String> = emptyList(),
    val actions: List<MonsterAction> = emptyList()
)

@Serializable
data class MonsterAction(
    val name: String,
    val description: String? = null,
    val attackBonus: Int? = null,
    val range: String? = null,
    val damage: List<MonsterDamage> = emptyList(),
    val actionIndex: String? = null,
    val triggers: List<EffectTrigger> = emptyList()
)

@Serializable
data class MonsterDamage(
    val dice: String,
    val type: String? = null
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\MonsterModels.kt
