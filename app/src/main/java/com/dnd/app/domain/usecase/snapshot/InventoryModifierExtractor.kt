// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\InventoryModifierExtractor.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import android.util.Log
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InventoryModifierExtractor @Inject constructor(
    private val json: Json
) {
    private val TAG = "ModifierExtractor"

    fun extract(inventory: List<InventoryItemSnapshot>, equippedIds: Set<String>, attunedIds: Set<String>): ModifierRegistry {
        var acBonus = 0
        var saveBonus = 0
        var profMod = 0
        val statOverrides = mutableMapOf<String, Int>()
        val statBonuses = mutableMapOf<String, Int>()
        val skillBonuses = mutableMapOf<String, Int>()
        val flags = mutableSetOf<String>()

        inventory.filter { it.isActive(equippedIds, attunedIds) }.forEach { item ->

            acBonus += item.magicBonusAc


            item.mechanicsJson?.let { raw ->
                runCatching {
                    val root = json.parseToJsonElement(raw).jsonObject


                    val ac = root["ac_bonus"] ?: root["armor_class_bonus"]
                    ac?.jsonPrimitive?.intOrNull?.let { acBonus += it }


                    val saves = root["save_bonus"] ?: root["saving_throw_bonus"] ?: root["saving_throws_bonus"]
                    saves?.jsonPrimitive?.intOrNull?.let { saveBonus += it }


                    root["prof_bonus_mod"]?.jsonPrimitive?.intOrNull?.let { profMod += it }


                    root["skill_bonuses"]?.jsonObject?.forEach { (skill, bonus) ->
                        val current = skillBonuses[skill] ?: 0
                        skillBonuses[skill] = current + (bonus.jsonPrimitive.intOrNull ?: 0)
                    }


                    root["flags"]?.jsonArray?.forEach {
                        flags.add(it.jsonPrimitive.content)
                    }
                }.onFailure { Log.e(TAG, "Mechanics parsing failed for ${item.name}") }
            }


            item.statOverridesJson?.let { raw ->
                runCatching {
                    val root = json.parseToJsonElement(raw).jsonObject
                    root.forEach { (stat, value) ->
                        val code = stat.uppercase().take(3)
                        val newVal = value.jsonPrimitive.intOrNull ?: 0
                        val current = statOverrides[code] ?: 0

                        if (newVal > current) statOverrides[code] = newVal
                    }
                }.onFailure { Log.e(TAG, "Stat overrides parsing failed for ${item.name}") }
            }
        }

        return ModifierRegistry(
            acBonus = acBonus,
            saveBonus = saveBonus,
            profBonusMod = profMod,
            statOverrides = statOverrides,
            statBonuses = statBonuses,
            skillBonuses = skillBonuses,
            specialFlags = flags
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\InventoryModifierExtractor.kt