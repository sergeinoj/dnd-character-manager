// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\ResourceRegistryAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.model.ClassSpecificJson
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import com.dnd.app.domain.model.snapshot.ResetRule
import com.dnd.app.domain.model.snapshot.ResourcePoolSnapshot
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max


@Singleton
class ResourceRegistryAssembler @Inject constructor(
    private val json: Json
) {

    companion object {
        private const val COLOR_RAGE = 0xFFC62828
        private const val COLOR_KI = 0xFFEF6C00
        private const val COLOR_DIVINITY = 0xFFFBC02D
        private const val COLOR_HEAL = 0xFF43A047
        private const val COLOR_MAGIC = 0xFF7B1FA2
        private const val COLOR_DEFAULT = 0xFF1976D2
    }

    fun assemble(
        latestProgression: Map<String, ProgressionEntity?>,
        activeItems: List<InventoryItemSnapshot>,
        features: List<Feature>,
        statModifiers: Map<String, Int>
    ): List<ResourcePoolSnapshot> {
        val pools = mutableListOf<ResourcePoolSnapshot>()
        val progressionResourceNames = mutableSetOf<String>()


        latestProgression.forEach { (classIdx, row) ->
            val dynamicRules = row?.classSpecificJson
                ?.let { raw -> runCatching { json.decodeFromString<ClassSpecificJson>(raw) }.getOrNull() }
                ?.resourceRules
                .orEmpty()

            fun resolveDynamicMax(resourceName: String?, fallback: Int): Int {
                if (resourceName.isNullOrBlank()) return fallback
                val normalizedName = normalizeResourceName(resourceName) ?: return fallback
                val matchedRule = dynamicRules.firstOrNull { rule ->
                    val ruleName = normalizeResourceName(rule.poolName)
                    ruleName == null || ruleName == normalizedName
                } ?: return fallback
                val statCode = matchedRule.stat.uppercase()
                val statMod = statModifiers[statCode] ?: 0
                val calculated = when (matchedRule.formula.uppercase()) {
                    "MOD" -> statMod
                    "LEVEL_PLUS_MOD" -> (row?.level ?: 0) + statMod
                    "HALF_LEVEL_PLUS_MOD" -> ((row?.level ?: 0) / 2) + statMod
                    else -> fallback
                }
                val withMin = max(matchedRule.minLimit, calculated)
                return matchedRule.maxLimit?.let { withMin.coerceAtMost(it) } ?: withMin
            }

            if (row != null && row.maxCharges > 0 && !row.resourceName.isNullOrBlank()) {

                val localizedName = DndLocalization.translateProficiency(row.resourceName)
                val resolvedMax = resolveDynamicMax(row.resourceName, row.maxCharges)

                pools.add(
                    ResourcePoolSnapshot(
                        id = "class_res_1_$classIdx",
                        name = localizedName,
                        max = resolvedMax,
                        resetRule = parseResetRule(row.chargeResetRule),
                        displayPriority = 10,
                        uiColorHex = mapResourceToColor(row.resourceName)
                    )
                )
                recordNormalizedNames(progressionResourceNames, localizedName, row.resourceName)
            }
            if (row != null && row.maxCharges2 > 0 && !row.resourceName2.isNullOrBlank()) {
                val localizedName2 = DndLocalization.translateProficiency(row.resourceName2)
                val resolvedMax2 = resolveDynamicMax(row.resourceName2, row.maxCharges2)
                pools.add(
                    ResourcePoolSnapshot(
                        id = "class_res_2_$classIdx",
                        name = localizedName2,
                        max = resolvedMax2,
                        resetRule = parseResetRule(row.chargeResetRule2),
                        displayPriority = 11,
                        uiColorHex = mapResourceToColor(row.resourceName2)
                    )
                )
                recordNormalizedNames(progressionResourceNames, localizedName2, row.resourceName2)
            }
        }



        activeItems.filter { it.maxCharges > 0 }.forEach { item ->
            pools.add(
                ResourcePoolSnapshot(
                    id = item.uniqueId,
                    name = item.name,
                    max = item.maxCharges,
                    resetRule = item.resetRule,
                    displayPriority = 100,
                    uiColorHex = COLOR_MAGIC
                )
            )
        }

        features
            .filter { it.maxCharges > 0 && it.index.isNotBlank() }
            .forEach { feature ->
                val featureNameNorm = normalizeResourceName(feature.name)
                val featureIndexNorm = normalizeResourceName(feature.index)
                val duplicatesProgression = (featureNameNorm != null && featureNameNorm in progressionResourceNames) ||
                    (featureIndexNorm != null && featureIndexNorm in progressionResourceNames)
                if (duplicatesProgression) return@forEach
                pools.add(
                    ResourcePoolSnapshot(
                        id = feature.index,
                        name = feature.name.ifBlank { DndLocalization.translateProficiency(feature.index) },
                        max = feature.maxCharges,
                        resetRule = feature.resetRule,
                        displayPriority = 20,
                        uiColorHex = COLOR_DEFAULT
                    )
                )
            }

        return pools.distinctBy { it.id }.sortedBy { it.displayPriority }
    }

    private fun mapResourceToColor(name: String): Long {
        val lower = name.lowercase()
        return when {
            lower.contains("ярость") || lower.contains("rage") -> COLOR_RAGE
            lower.contains("ки") || lower.contains("ki") || lower.contains("ци") -> COLOR_KI
            lower.contains("наложение рук") || lower.contains("lay on hands") -> COLOR_HEAL
            lower.contains("божественн") || lower.contains("divinity") -> COLOR_DIVINITY
            else -> COLOR_DEFAULT
        }
    }

    private fun normalizeResourceName(name: String?): String? {
        return name
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
    }

    private fun recordNormalizedNames(target: MutableSet<String>, vararg names: String?) {
        names.forEach { normalizeResourceName(it)?.let(target::add) }
    }

    private fun parseResetRule(rule: String?): ResetRule = when (rule?.uppercase()) {
        "SHORT_REST" -> ResetRule.SHORT_REST
        "DAWN" -> ResetRule.DAWN
        "NEVER" -> ResetRule.NEVER
        else -> ResetRule.LONG_REST
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\ResourceRegistryAssembler.kt
