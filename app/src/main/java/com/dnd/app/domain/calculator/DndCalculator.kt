// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\calculator\DndCalculator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.calculator

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.LevelStep
import com.dnd.app.domain.model.snapshot.MagicSourceType
import com.dnd.app.domain.model.snapshot.PactMagicSnapshot
import com.dnd.app.domain.model.snapshot.PreparationMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.max

@Singleton
class DndCalculator @Inject constructor() {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    data class ItemMagicMeta(
        val spellIndexes: List<String>,
        val dc: Int,
        val atk: Int?,
        val stat: String?,
        val maxCharges: Int
    )

    companion object {
        private val multiclassSpellSlotTable = listOf(
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(3, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(4, 2, 0, 0, 0, 0, 0, 0, 0), intArrayOf(4, 3, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(4, 3, 2, 0, 0, 0, 0, 0, 0), intArrayOf(4, 3, 3, 0, 0, 0, 0, 0, 0),
            intArrayOf(4, 3, 3, 1, 0, 0, 0, 0, 0), intArrayOf(4, 3, 3, 2, 0, 0, 0, 0, 0),
            intArrayOf(4, 3, 3, 3, 1, 0, 0, 0, 0), intArrayOf(4, 3, 3, 3, 2, 0, 0, 0, 0),
            intArrayOf(4, 3, 3, 3, 2, 1, 0, 0, 0), intArrayOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
            intArrayOf(4, 3, 3, 3, 2, 1, 1, 0, 0), intArrayOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
            intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 0), intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
            intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 1), intArrayOf(4, 3, 3, 3, 3, 1, 1, 1, 1),
            intArrayOf(4, 3, 3, 3, 3, 2, 1, 1, 1), intArrayOf(4, 3, 3, 3, 3, 2, 2, 1, 1)
        )

        private val PACT_PROGRESSION_TABLE: Map<Int, Pair<Int, Int>> = mapOf(
            1 to (1 to 1), 2 to (2 to 1), 3 to (2 to 2), 4 to (2 to 2), 5 to (2 to 3),
            6 to (2 to 3), 7 to (2 to 4), 8 to (2 to 4), 9 to (2 to 5), 10 to (2 to 5),
            11 to (3 to 5), 12 to (3 to 5), 13 to (3 to 5), 14 to (3 to 5), 15 to (3 to 5),
            16 to (3 to 5), 17 to (4 to 5), 18 to (4 to 5), 19 to (4 to 5), 20 to (4 to 5)
        )
    }

    fun calculateModifier(score: Int): Int = floor((score - 10) / 2.0).toInt()


    fun calculateEffectiveCasterLevel(
        progressionRows: List<ProgressionEntity>,
        excludedClassIndexes: Set<String> = emptySet(),
        sourceTypeByIndex: Map<String, MagicSourceType> = emptyMap()
    ): Int {
        val normalizedExclusions = excludedClassIndexes.map { it.lowercase() }.toSet()
        val normalizedSourceTypes = sourceTypeByIndex.mapKeys { it.key.trim().lowercase() }
        val filteredRows = progressionRows.filter { row ->
            val key = row.classIndex.trim().lowercase()
            if (key.isBlank() || key in normalizedExclusions) return@filter false
            val sourceType = normalizedSourceTypes[key]
            if (sourceType == MagicSourceType.RACE || sourceType == MagicSourceType.ITEM) return@filter false
            true
        }
        val distinctRows = filteredRows
            .groupBy { "${it.classIndex}_${it.level}" }
            .values
            .map { it.first() }

        val totalEcl = distinctRows.sumOf { it.casterLevelIncrement }
        return floor(totalEcl).toInt()
    }

    fun calculatePactMagic(progressionRows: List<ProgressionEntity>): PactMagicSnapshot? {
        val pactLevels = progressionRows.count { it.isPactIncrement == 1 }
        if (pactLevels <= 0) return null
        val (slots, slotLevel) = PACT_PROGRESSION_TABLE[pactLevels.coerceIn(1, 20)] ?: (1 to 1)
        return PactMagicSnapshot(maxSlots = slots, slotLevel = slotLevel)
    }

    fun getPreparationMode(casterType: String?): PreparationMode = when (casterType?.uppercase()) {
        "PREPARED" -> PreparationMode.PREPARED
        "KNOWN" -> PreparationMode.KNOWN
        "PACT" -> PreparationMode.KNOWN
        else -> PreparationMode.NONE
    }

    fun calculateMaxPrepared(
        formulaType: String?,
        level: Int,
        modifier: Int
    ): Int = when (formulaType) {
        "FULL" -> max(1, level + modifier)
        "HALF" -> max(1, (level / 2) + modifier)
        else -> 0
    }


    fun calculateRelevantAbilityModifier(draft: DraftCharacter, classMetadata: Map<String, ClassEntity>): Int {
        val lastStep = draft.levelStack.lastOrNull() ?: return 0
        val metadata = classMetadata[lastStep.classIndex] ?: return 0
        val statCode = metadata.primaryStat ?: "CHA"

        val baseScore = draft.baseInfo.baseAbilityScores[statCode] ?: 10
        val bonus = draft.baseInfo.aggregateStatBonuses[statCode] ?: 0
        return calculateModifier(baseScore + bonus)
    }


    fun resolvePreparationFormula(formula: String, level: Int, modifier: Int, minLimit: Int): Int {
        val result = when (formula.uppercase()) {
            "LEVEL_PLUS_MOD" -> level + modifier
            "HALF_LEVEL_PLUS_MOD" -> (level / 2) + modifier
            else -> modifier
        }
        return max(minLimit, result)
    }

    fun calculateProficiencyBonus(totalLevel: Int): Int = when {
        totalLevel >= 17 -> 6; totalLevel >= 13 -> 5; totalLevel >= 9 -> 4; totalLevel >= 5 -> 3; else -> 2
    }

    fun formatModifier(mod: Int): String = if (mod >= 0) "+$mod" else "$mod"

    fun calculateSkillBonus(score: Int, profBonus: Int, multiplier: Int): Int =
        calculateModifier(score) + (profBonus * multiplier)

    fun getGlobalSpellSlots(ecl: Int): Map<Int, Int> {
        if (ecl <= 0) return emptyMap()
        val tableIndex = (ecl - 1).coerceIn(0, multiclassSpellSlotTable.lastIndex)
        return multiclassSpellSlotTable[tableIndex].mapIndexed { i, count -> (i + 1) to count }.filter { it.second > 0 }.toMap()
    }

    fun getMaxSpellLevel(classIndex: String, level: Int): Int {
        // Treat warlock/pact classes differently: use PACT_PROGRESSION_TABLE for slot level
        val lower = classIndex.lowercase()
        return if (lower.contains("warlock")) {
            val slotLevel = PACT_PROGRESSION_TABLE[level.coerceIn(1, 20)]?.second ?: 1
            slotLevel
        } else {
            val slots = getGlobalSpellSlots(level)
            slots.keys.maxOrNull() ?: 0
        }
    }

    fun isPurePactCaster(
        levelStack: List<LevelStep>,
        classMetadata: Map<String, ClassEntity>,
        excludedClassIndexes: Set<String> = emptySet(),
        sourceTypeByIndex: Map<String, MagicSourceType> = emptyMap()
    ): Boolean {
        if (levelStack.isEmpty()) return false

        val normalizedExclusions = excludedClassIndexes.map { it.lowercase() }.toSet()
        val normalizedSourceTypes = sourceTypeByIndex.mapKeys { it.key.trim().lowercase() }
        val stepsMetadata = levelStack.mapNotNull { step ->
            val key = step.classIndex.trim().lowercase()
            if (key in normalizedExclusions) return@mapNotNull null
            val sourceType = normalizedSourceTypes[key]
            if (sourceType == MagicSourceType.RACE || sourceType == MagicSourceType.ITEM) return@mapNotNull null
            classMetadata[step.classIndex]
        }
        if (stepsMetadata.isEmpty()) return false

        val hasPactClass = stepsMetadata.any { it.casterType?.equals("PACT", ignoreCase = true) == true }
        if (!hasPactClass) return false

        val hasOtherCaster = stepsMetadata.any { entity ->
            val casterType = entity.casterType?.uppercase()?.takeIf { it.isNotBlank() }
            casterType != null && casterType != "PACT"
        }
        return !hasOtherCaster
    }

    fun resolveItemMagic(referenceJson: String?): ItemMagicMeta? {
        if (referenceJson.isNullOrBlank()) return null
        return try {
            val obj = jsonParser.parseToJsonElement(referenceJson).jsonObject
            val spells = obj["granted_spells"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val dc = obj["dc"]?.jsonPrimitive?.intOrNull ?: 13
            val atk = obj["attack_bonus"]?.jsonPrimitive?.intOrNull
            val stat = obj["casting_stat"]?.jsonPrimitive?.content
            val charges = obj["charges"]?.jsonPrimitive?.intOrNull ?: obj["max_charges"]?.jsonPrimitive?.intOrNull ?: 0
            if (spells.isNotEmpty() || obj.containsKey("dc") || charges > 0) ItemMagicMeta(spells, dc, atk, stat, charges) else null
        } catch (e: Exception) { null }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\calculator\DndCalculator.kt