// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\InventoryItemSnapshot.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.snapshot

import kotlinx.serialization.Serializable

@Serializable
enum class EquipSlot {
    NONE, WEAPON, ARMOR, SHIELD, ACCESSORY, CONSUMABLE, OTHER
}

@Serializable
data class InventoryItemSnapshot(
    val uniqueId: String,
    val traceKey: String,
    val refId: String?,
    val name: String,
    val description: String,
    val weight: Double,
    val cost: String,
    val quantity: Int,
    val containerId: String? = null,
    val isPack: Boolean = false,
    val damage: String?,
    val damageType: String?,
    val acBonus: Int?,
    val baseAc: Int?,
    val dexCap: Int?,
    val versatileDamage: String? = null,
    val isAttunementRequired: Boolean,
    val maxCharges: Int,
    val poolId: String?,
    val resetRule: ResetRule,
    val magicBonusAc: Int = 0,
    val magicBonusAttack: Int = 0,
    val magicBonusDamage: Int = 0,
    val magicBonusSaveDc: Int = 0,
    val grantedSpells: List<String> = emptyList(),
    val equipSlot: EquipSlot = EquipSlot.NONE,
    val properties: List<String> = emptyList(),
    val requirements: Map<String, Int> = emptyMap(),
    val scalingStat: String? = null,
    val isStartingEquipment: Boolean = false,
    val hitBonus: Int? = null,
    val damageBonus: Int? = null,
    val baseUnitCostCp: Int = 0,
    val referenceJson: String? = null,
    val rarity: String? = null,
    val statOverridesJson: String? = null,
    val mechanicsJson: String? = null,
    val variant: Int? = null,
    val category: String? = null
) {
    fun isActive(equippedIds: Set<String>, attunedIds: Set<String>): Boolean {

        if (containerId != null) return false
        val isEquipped = uniqueId in equippedIds
        val isAttuned = !isAttunementRequired || (uniqueId in attunedIds)
        return isEquipped && isAttuned
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\InventoryItemSnapshot.kt