// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\models.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import com.dnd.app.domain.model.snapshot.ResetRule

sealed class InventoryException(message: String) : Exception(message)

class ItemNotFoundException(message: String) : InventoryException(message)
class NotEnoughGoldException(message: String = "Недостаточно золота для совершения покупки") : InventoryException(message)
class OverEncumberedException(message: String = "Предмет слишком тяжелый для переноски") : InventoryException(message)
class InconsistentDataException(message: String) : InventoryException(message)
class ItemUnpackException(message: String) : InventoryException(message)

data class RawItemData(
    val indexName: String,
    val name: String,
    val weight: Double?,
    val costCp: Int,
    val categoryIndex: String? = null,
    val description: String? = null,
    val damageDice: String? = null,
    val damageType: String? = null,
    val propertiesJson: String? = null,
    val baseAc: Int? = null,
    val dexCap: Int? = null,
    val strMinimum: Int? = null,
    val stealthDisadvantage: Boolean = false,
    val contentsJson: String? = null,
    val baseItemIndex: String? = null,
    val requiresAttunement: Boolean = false,
    val maxCharges: Int = 0,
    val chargeResetRule: String? = null,
    val bonusAc: Int = 0,
    val bonusAttack: Int = 0,
    val bonusDamage: Int = 0,
    val bonusSaveDc: Int = 0,
    val grantedSpellsJson: String? = null,
    val referenceJson: String? = null,
    val rarity: String? = null,
    val statOverridesJson: String? = null,
    val mechanicsJson: String? = null,
    val variant: Int? = null
)

data class UnpackedItem(
    val itemId: String,
    val sourceKey: String,
    val uniqueId: String,
    val name: String,
    val weight: Double,
    val description: String,
    val costCp: Int,
    val quantity: Int = 1,
    val isPack: Boolean = false,
    val containerId: String? = null,
    val totalContentsWeight: Double? = null,
    val damage: String?,
    val damageType: String?,
    val properties: List<String>,
    val baseAc: Int?,
    val dexCap: Int?,
    val isShield: Boolean,
    val stealthDisadvantage: Boolean,
    val strMinimum: Int?,
    val isAttunementRequired: Boolean,
    val maxCharges: Int,
    val resetRule: ResetRule,
    val bonusAc: Int,
    val bonusAttack: Int,
    val bonusDamage: Int,
    val bonusSaveDc: Int,
    val grantedSpells: List<String>,
    val referenceJson: String?,
    val categoryIndex: String?,
    val rarity: String?,
    val statOverridesJson: String?,
    val mechanicsJson: String?,
    val variant: Int?
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\models.kt