// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\VitalsAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.model.snapshot.ClassLevelSnapshot
import com.dnd.app.domain.model.snapshot.EquipSlot
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton


data class VitalsReport(
    val maxHp: Int,
    val currentHp: Int,
    val tempHp: Int,
    val hitDice: String,
    val hitDiceCount: Int,
    val finalSpeed: Int,
    val classSnapshots: List<ClassLevelSnapshot>,
    val classTitle: String,
    val canWildShape: Boolean
)


@Singleton
class VitalsAssembler @Inject constructor(
    private val json: Json
) {

    fun assemble(
        draft: DraftCharacter,
        statModifiers: Map<String, Int>,
        classMetadata: Map<String, ClassEntity>,
        race: Race?,
        weightReport: WeightReport,
        oldLiveState: CharacterLiveState?,
        oldSnapshot: CharacterSnapshot?,
        entityCurrentHp: Int?,
        featureHpBonus: Int,
        progressionRows: List<ProgressionEntity>,
        activeFeatures: List<Feature>,
        inventory: List<InventoryItemSnapshot>,
        equippedIds: Set<String>,
        exhaustionLevel: Int = 0
    ): VitalsReport {
        val conModifier = statModifiers["CON"] ?: 0
        val totalLevel = draft.levelStack.size.coerceAtLeast(1)


        val baseMaxHp = draft.levelStack.foldIndexed(0) { i, acc, step ->
            val gain = if (i == 0) {
                classMetadata[step.classIndex]?.hitDie ?: 8
            } else {
                step.hpIncrease
            }
            acc + gain + conModifier
        }

        var finalMaxHp = baseMaxHp + featureHpBonus
        if (exhaustionLevel >= 4) {
            finalMaxHp = finalMaxHp / 2
        }



        val baseHp = entityCurrentHp ?: oldLiveState?.hpCurrent ?: finalMaxHp
        val currentHp = (baseHp + (finalMaxHp - (oldSnapshot?.maxHp ?: finalMaxHp))).coerceIn(0, finalMaxHp)


        val hitDiceString = draft.levelStack
            .map { classMetadata[it.classIndex]?.hitDie ?: 8 }
            .groupBy { it }
            .entries
            .sortedByDescending { it.key }
            .joinToString(" + ") { "${it.value.size}к${it.key}" }


        val equippedItems = inventory.filter { it.uniqueId in equippedIds }
        val hasArmor = equippedItems.any { it.equipSlot == EquipSlot.ARMOR }
        val hasHeavyArmor = equippedItems.any(::isHeavyArmor)
        val hasShield = equippedItems.any { it.equipSlot == EquipSlot.SHIELD }
        val unarmoredBonus = resolveUnarmoredSpeedBonus(
            features = activeFeatures,
            progressionRows = progressionRows,
            hasArmor = hasArmor,
            hasHeavyArmor = hasHeavyArmor,
            hasShield = hasShield
        )
        var finalSpeed = ((race?.speed ?: 30) + unarmoredBonus - weightReport.speedPenalty).coerceAtLeast(0)
        when {
            exhaustionLevel >= 5 -> finalSpeed = 0
            exhaustionLevel >= 2 -> finalSpeed = finalSpeed / 2
        }


        val classSnapshots = draft.levelStack
            .groupBy { it.classIndex }
            .map { (idx, steps) ->
                ClassLevelSnapshot(classMetadata[idx]?.name ?: idx, steps.size)
            }
        val classTitle = classSnapshots.joinToString(" / ") { "${it.className} ${it.level}" }
        val canWildShape = activeFeatures.any(::isWildShapeFeature)

        return VitalsReport(
            maxHp = finalMaxHp,
            currentHp = currentHp,
            tempHp = oldLiveState?.hpTemp ?: 0,
            hitDice = hitDiceString,
            hitDiceCount = totalLevel,
            finalSpeed = finalSpeed,
            classSnapshots = classSnapshots,
            classTitle = classTitle,
            canWildShape = canWildShape
        )
    }

    private fun resolveUnarmoredSpeedBonus(
        features: List<Feature>,
        progressionRows: List<ProgressionEntity>,
        hasArmor: Boolean,
        hasHeavyArmor: Boolean,
        hasShield: Boolean
    ): Int {
        var bestBonus = 0
        features.forEach { feature ->
            val raw = feature.referenceJson ?: return@forEach
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject
                val mech = root["mechanics"] as? JsonObject ?: return@forEach
                val type = mech["type"]?.jsonPrimitive?.content?.uppercase() ?: return@forEach
                if (type != "MODIFY_PROPERTY") return@forEach
                val statFilter = mech["stat_filter"] as? JsonArray ?: return@forEach
                val hasSpeed = statFilter.any { it.jsonPrimitive.contentOrNull == "SPEED" }
                if (!hasSpeed) return@forEach
                val conds = (
                    (root["conditions"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty() +
                        (mech["conditions"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                    )
                    .map { it.normalizeConditionToken() }
                    .toSet()
                if (hasArmor && "no_armor" in conds) return@forEach
                if (hasShield && "no_shield" in conds) return@forEach
                if (hasHeavyArmor && ("no_heavy_armor" in conds || "not_heavy_armor" in conds)) return@forEach
                val baseBonus = mech["scaling_bonus"]?.jsonPrimitive?.intOrNull ?: 0
                val classIdx = feature.classIndex
                val classBonus = classIdx?.let { idx ->
                    progressionRows.filter { it.classIndex == idx }.maxOfOrNull { it.movementBonus } ?: 0
                } ?: 0
                val totalBonus = if (classBonus > 0) classBonus else baseBonus
                if (totalBonus > bestBonus) bestBonus = totalBonus
            }
        }
        return bestBonus
    }

    private fun isHeavyArmor(item: InventoryItemSnapshot): Boolean {
        if (item.equipSlot != EquipSlot.ARMOR) return false

        val textSignals = buildList {
            add(item.category.orEmpty())
            add(item.refId.orEmpty())
            add(item.name)
            add(item.properties.joinToString(" "))
        }.joinToString(" ").lowercase()

        if (textSignals.contains("heavy") || textSignals.contains("тяж")) return true

        val rawJson = item.referenceJson ?: return false
        val root = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return false
        return containsHeavyToken(root)
    }

    private fun containsHeavyToken(node: kotlinx.serialization.json.JsonElement): Boolean {
        return when (node) {
            is JsonObject -> node.any { (key, value) ->
                val keyNorm = key.normalizeConditionToken()
                val keyIsArmorField = keyNorm in setOf("armor_category", "armor_type", "category", "armor")
                (keyIsArmorField && primitiveContainsHeavy(value as? JsonPrimitive)) || containsHeavyToken(value)
            }
            is JsonArray -> node.any { containsHeavyToken(it) }
            is JsonPrimitive -> primitiveContainsHeavy(node)
        }
    }

    private fun primitiveContainsHeavy(primitive: JsonPrimitive?): Boolean {
        val raw = primitive?.contentOrNull?.lowercase() ?: return false
        return raw.contains("heavy") || raw.contains("тяж")
    }

    private fun String.normalizeConditionToken(): String = lowercase().replace('-', '_').trim()

    private fun isWildShapeFeature(feature: Feature): Boolean {
        if (feature.index.equals("wild-shape", ignoreCase = true)) return true
        return feature.name.contains("wild shape", ignoreCase = true)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\VitalsAssembler.kt
