// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\ItemFabricator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.snapshot.EquipSlot
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import com.dnd.app.domain.model.snapshot.StatModel
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ItemFabricator @Inject constructor(
    private val calculator: DndCalculator,
    private val json: Json
) {

    fun fabricate(
        item: UnpackedItem,
        statsMap: Map<String, StatModel>,
        uniqueId: String,
        traceKey: String,
        baseUnitCostCp: Int,
        isStarting: Boolean = false
    ): InventoryItemSnapshot {
        val strMod = calculator.calculateModifier(statsMap["STR"]?.value ?: 10)
        val dexMod = calculator.calculateModifier(statsMap["DEX"]?.value ?: 10)

        val finalSlot = determineEquipSlot(
            itemId = item.itemId,
            itemName = item.name,
            category = item.categoryIndex,
            damage = item.damage,
            isShield = item.isShield,
            baseAc = item.baseAc,
            isAttunementRequired = item.isAttunementRequired,
            bonusAc = item.bonusAc,
            bonusAttack = item.bonusAttack,
            bonusDamage = item.bonusDamage,
            bonusSaveDc = item.bonusSaveDc,
            rarity = item.rarity
        )


        val versatileDmg = if (item.properties.any { it.contains("versatile", ignoreCase = true) || it.contains("универсальное", ignoreCase = true) }) {
            upgradeDice(item.damage)
        } else null

        val castingStat = extractCastingStat(item.referenceJson)
        val scaling = when {
            castingStat != null -> castingStat
            item.properties.any { it.contains("finesse", ignoreCase = true) || it.contains("фехтовальное", ignoreCase = true) } && dexMod > strMod -> "DEX"
            else -> "STR"
        }

        return InventoryItemSnapshot(
            uniqueId = uniqueId,
            traceKey = traceKey,
            refId = item.itemId,
            name = item.name,
            description = item.description,
            weight = item.weight,
            cost = if (item.costCp > 0) item.costCp.toString() else "Бесценно",
            baseUnitCostCp = baseUnitCostCp,
            quantity = item.quantity,
            containerId = item.containerId,
            isPack = item.isPack,
            damage = item.damage,
            damageType = item.damageType,
            acBonus = if (item.isShield) (item.baseAc ?: 2) else null,
            baseAc = if (!item.isShield) item.baseAc else null,
            dexCap = item.dexCap,
            versatileDamage = versatileDmg,
            isAttunementRequired = item.isAttunementRequired,
            maxCharges = item.maxCharges,
            poolId = if (item.maxCharges > 0) uniqueId else null,
            resetRule = item.resetRule,
            magicBonusAc = item.bonusAc,
            magicBonusAttack = item.bonusAttack,
            magicBonusDamage = item.bonusDamage,
            magicBonusSaveDc = item.bonusSaveDc,
            grantedSpells = item.grantedSpells,
            equipSlot = finalSlot,
            properties = item.properties,
            requirements = if ((item.strMinimum ?: 0) > 0) mapOf("STR" to item.strMinimum!!) else emptyMap(),
            scalingStat = scaling,
            isStartingEquipment = isStarting,
            hitBonus = item.bonusAttack,
            damageBonus = item.bonusDamage,
            referenceJson = item.referenceJson,
            rarity = item.rarity,
            statOverridesJson = item.statOverridesJson,
            mechanicsJson = item.mechanicsJson,
            variant = item.variant,
            category = item.categoryIndex
        )
    }

    fun reFabricate(oldItem: InventoryItemSnapshot): InventoryItemSnapshot {
        val newSlot = determineEquipSlot(
            itemId = oldItem.refId ?: "",
            itemName = oldItem.name,
            category = oldItem.category,
            damage = oldItem.damage,
            isShield = oldItem.category?.contains("shield", ignoreCase = true) == true || (oldItem.acBonus != null && oldItem.baseAc == null),
            baseAc = oldItem.baseAc,
            isAttunementRequired = oldItem.isAttunementRequired,
            bonusAc = oldItem.magicBonusAc,
            bonusAttack = oldItem.magicBonusAttack,
            bonusDamage = oldItem.magicBonusDamage,
            bonusSaveDc = oldItem.magicBonusSaveDc,
            rarity = oldItem.rarity
        )
        return oldItem.copy(equipSlot = newSlot)
    }

    private fun determineEquipSlot(
        itemId: String,
        itemName: String,
        category: String?,
        damage: String?,
        isShield: Boolean,
        baseAc: Int?,
        isAttunementRequired: Boolean,
        bonusAc: Int,
        bonusAttack: Int,
        bonusDamage: Int,
        bonusSaveDc: Int,
        rarity: String?
    ): EquipSlot {
        val cat = (category ?: "").lowercase()
        val id = itemId.lowercase()
        val nameLower = itemName.lowercase()

        if (!damage.isNullOrBlank() || cat.contains("weapon")) return EquipSlot.WEAPON
        if ((baseAc != null && !isShield) || cat.contains("armor")) return EquipSlot.ARMOR
        if (isShield || cat.contains("shield")) return EquipSlot.SHIELD

        val isStrictRing = (cat == "ring" || cat == "rings") || (cat.contains("ring") && !cat.contains("adventuring"))
        val isMagical = isAttunementRequired || rarity != null || bonusAc > 0 || bonusAttack > 0 || bonusDamage > 0 || bonusSaveDc > 0

        val isAccessoryKeyword = isStrictRing || cat.contains("wand") || cat.contains("staff") ||
                cat.contains("cloak") || cat.contains("amulet") ||
                nameLower.contains("кольцо") || nameLower.contains("ring") ||
                nameLower.contains("плащ") || nameLower.contains("cloak") ||
                nameLower.contains("амулет") || nameLower.contains("amulet") ||
                nameLower.contains("пояс") || nameLower.contains("belt")

        if (isMagical || isAccessoryKeyword) return EquipSlot.ACCESSORY

        val isFocusOrSymbol = cat.contains("focus") || cat.contains("foci") ||
                cat.contains("symbol") || id.contains("focus") ||
                id.contains("symbol") || id.contains("amulet") ||
                nameLower.contains("символ") || nameLower.contains("symbol") ||
                nameLower.contains("фокусировка") || nameLower.contains("reliquary")

        val isTool = cat.contains("tool") || cat.contains("instrument") ||
                cat.contains("kit") || cat.contains("gaming") ||
                id.contains("tool") || id.contains("kit")

        if (isFocusOrSymbol || isTool) return EquipSlot.OTHER

        return EquipSlot.NONE
    }

    private fun upgradeDice(base: String?): String? {
        if (base.isNullOrBlank()) return null
        val separator = if (base.contains('d', ignoreCase = true)) 'd' else if (base.contains('к', ignoreCase = true)) 'к' else null
        if (separator == null) return base

        val parts = base.lowercase().split(separator)
        if (parts.size != 2) return base

        val count = parts[0].trim()
        val sidesPart = parts[1].trim().takeWhile { it.isDigit() }
        val sides = sidesPart.toIntOrNull() ?: return base

        val upgraded = when (sides) {
            4 -> 6
            6 -> 8
            8 -> 10
            10 -> 12
            else -> sides
        }

        return "${count}${separator}${upgraded}"
    }

    private fun extractCastingStat(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val obj = json.parseToJsonElement(rawJson).jsonObject
            obj["casting_stat"]?.jsonPrimitive?.content?.uppercase()
                ?: obj["stat_overrides"]?.jsonObject?.keys?.firstOrNull()?.uppercase()
        }.getOrNull()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\ItemFabricator.kt