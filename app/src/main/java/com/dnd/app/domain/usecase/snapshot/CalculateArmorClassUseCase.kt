package com.dnd.app.domain.usecase.snapshot

import android.util.Log
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.snapshot.EquipSlot
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class CalculateArmorClassUseCase @Inject constructor() {
    private val tag = "DND_AC_CALC"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    operator fun invoke(
        inventory: List<InventoryItemSnapshot>,
        equippedIds: Set<String>,
        statModifiers: Map<String, Int>,
        activeFeatures: List<Feature>,
        extraAcBonus: Int = 0
    ): Int {
        val equippedItems = inventory.filter { it.uniqueId in equippedIds }
        val equippedArmor = equippedItems.find { it.equipSlot == EquipSlot.ARMOR }
        val equippedShield = equippedItems.find { it.equipSlot == EquipSlot.SHIELD }

        val hasArmor = equippedArmor != null
        val hasShield = equippedShield != null
        val shieldBonus = equippedShield?.acBonus ?: 0
        val dexModifier = statModifiers["DEX"] ?: 0

        val defaultUnarmoredAc = 10 + dexModifier + shieldBonus
        val formulaUnarmoredAc = resolveFormulaUnarmoredAc(
            features = activeFeatures,
            statModifiers = statModifiers,
            hasArmor = hasArmor,
            hasShield = hasShield,
            shieldBonus = shieldBonus
        )

        val armoredAc = if (equippedArmor != null) {
            val baseAc = equippedArmor.baseAc ?: 10
            val effectiveDexBonus = min(dexModifier, equippedArmor.dexCap ?: dexModifier)
            baseAc + effectiveDexBonus + shieldBonus
        } else {
            0
        }

        val bestUnarmoredAc = max(defaultUnarmoredAc, formulaUnarmoredAc)
        val finalBaseCalculatedAc = max(bestUnarmoredAc, armoredAc)
        return finalBaseCalculatedAc + extraAcBonus
    }

    private fun resolveFormulaUnarmoredAc(
        features: List<Feature>,
        statModifiers: Map<String, Int>,
        hasArmor: Boolean,
        hasShield: Boolean,
        shieldBonus: Int
    ): Int {
        var bestAc = 0
        val modRegex = Regex("""mod:([A-Z]{3})""")
        val plainStatRegex = Regex("""\b(STR|DEX|CON|INT|WIS|CHA)\b""")

        features.forEach { feature ->
            val raw = feature.referenceJson ?: return@forEach
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject
                val node = (root["mechanics"]?.jsonObject ?: root)
                val type = node["type"]?.jsonPrimitive?.contentOrNull?.lowercase()
                if (type != "ac_formula") return@runCatching

                val restrictions = (node["restrictions"]?.jsonArray ?: root["restrictions"]?.jsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.lowercase() }
                    .orEmpty()
                if (hasArmor && "no_armor" in restrictions) return@runCatching
                if (hasShield && "no_shield" in restrictions) return@runCatching

                val formula = node["formula"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
                val base = Regex("""\d+""").find(formula)?.value?.toIntOrNull() ?: 10
                val uppercaseFormula = formula.uppercase()
                val prefixedMods = modRegex.findAll(uppercaseFormula)
                    .map { it.groupValues[1] }
                    .toList()
                val plainMods = if (prefixedMods.isEmpty()) {
                    plainStatRegex.findAll(uppercaseFormula).map { it.groupValues[1] }.toList()
                } else {
                    emptyList()
                }
                val keys = (prefixedMods + plainMods).distinct()
                val formulaMods = keys.sumOf { statModifiers[it] ?: 0 }

                val candidateAc = base + formulaMods + shieldBonus
                if (candidateAc > bestAc) bestAc = candidateAc
                Log.d(tag, "AC formula applied: feature=${feature.index} formula='$formula' base=$base mods=$keys sum=$formulaMods shield=$shieldBonus => $candidateAc")
            }.onFailure { e ->
                Log.w(tag, "AC formula parse failed for feature=${feature.index}: ${e.message}")
            }
        }

        return bestAc
    }
}
