// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\DraftCharacter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class StaticProficiency(
    val id: String,
    val kind: ProficiencyKind
)

@Serializable
sealed class ChoiceResult {

    @Serializable
    data class Spells(val spellIndexes: List<String>) : ChoiceResult()

    @Serializable
    data class Skills(val skillIndexes: List<String>) : ChoiceResult()

    @Serializable
    data class SelectedOptions(
        val items: List<String>,
        override val targetProficiencyLevel: Int = 1,
        val proficiencyKind: ProficiencyKind = ProficiencyKind.NONE
    ) : ChoiceResult(), ProficiencyChoice

    @Serializable(with = StatBonusSerializer::class)
    data class StatBonus(val stats: List<String> = emptyList()) : ChoiceResult()

    object StatBonusSerializer : KSerializer<StatBonus> {
        private val statsSerializer = ListSerializer(String.serializer())
        private val bonusesSerializer = MapSerializer(String.serializer(), Int.serializer())

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StatBonus") {
            element("stats", statsSerializer.descriptor, isOptional = true)
            element("bonuses", bonusesSerializer.descriptor, isOptional = true)
        }

        override fun deserialize(decoder: Decoder): StatBonus {
            val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("StatBonus requires JsonDecoder")
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val statsElement = jsonObject["stats"]?.jsonArray

            val parsedStats = when {
                statsElement != null -> statsElement.mapNotNull { it.jsonPrimitive.contentOrNull?.uppercase() }
                jsonObject["bonuses"] != null -> parseBonuses(jsonObject["bonuses"]!!.jsonObject)
                else -> emptyList()
            }

            return StatBonus(parsedStats)
        }

        override fun serialize(encoder: Encoder, value: StatBonus) {
            val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("StatBonus requires JsonEncoder")
            val statsArray = JsonArray(value.stats.map { JsonPrimitive(it) })
            jsonEncoder.encodeJsonElement(buildJsonObject { put("stats", statsArray) })
        }

        private fun parseBonuses(bonuses: JsonObject): List<String> {
            return bonuses.flatMap { (stat, bonusValue) ->
                val count = bonusValue.jsonPrimitive.intOrNull ?: 0
                if (count <= 0) return@flatMap emptyList()
                List(count) { stat.uppercase() }
            }
        }
    }

    @Serializable
    data class Note(val text: String) : ChoiceResult()

    @Serializable
    data class RuleEffect(val effectType: String, val value: String) : ChoiceResult()
}

@Serializable
data class LevelStep(
    val classIndex: String,
    val subclassIndex: String? = null,
    val hpIncrease: Int = 0,
    val selections: Map<String, ChoiceResult> = emptyMap(),
    val autoSpells: List<String> = emptyList()
)

@Serializable
data class BaseInfo(
    val raceIndex: String = "",
    val subraceIndex: String? = null,
    val backgroundIndex: String = "",
    val alignmentIndex: String = "",
    val gender: String = "",
    val personalityTrait: String = "",
    val ideal: String = "",
    val bond: String = "",
    val flaw: String = "",
    val appearance: String = "",
    val backstory: String = "",
    val baseAbilityScores: Map<String, Int> = mapOf(
        "STR" to 8, "DEX" to 8, "CON" to 8,
        "INT" to 8, "WIS" to 8, "CHA" to 8
    ),
    val aggregateStatBonuses: Map<String, Int> = emptyMap(),
    val raceSelections: Map<String, ChoiceResult> = emptyMap(),
    val backgroundSelections: Map<String, ChoiceResult> = emptyMap(),
    val inventorySelections: Map<String, ChoiceResult> = emptyMap(),
    val staticProficiencies: List<StaticProficiency> = emptyList(),
    val staticEquipment: List<String> = emptyList(),
    val staticSpells: List<String> = emptyList(),
    val startingClassIndex: String = "",

    val startingGold: Int = 0
)

@Serializable
data class DraftCharacter(
    val id: Long = 0,
    val name: String = "",
    val baseInfo: BaseInfo = BaseInfo(),
    val levelStack: List<LevelStep> = emptyList()
) {
    fun getAllProficienciesWithLevels(): Map<String, Int> {
        return getAllProficienciesWithLevels(null)
    }

    fun getProficiencyExclusions(targetLevel: Int, currentKeyToExclude: String? = null): Set<String> {
        val allCurrentProficiencies = getAllProficienciesWithLevels(currentKeyToExclude)
        val exclusions = mutableSetOf<String>()

        for ((id, level) in allCurrentProficiencies) {
            val isBinary = id.startsWith("lang-") || id.startsWith("saving-throw-")

            if (isBinary) {
                exclusions.add(id)
                continue
            }

            when (targetLevel) {
                1 -> {
                    if (level >= 1) exclusions.add(id)
                }
                2 -> {
                    if (level == 2) exclusions.add(id)
                }
            }
        }
        return exclusions
    }

    private fun getAllProficienciesWithLevels(keyToExclude: String? = null): Map<String, Int> {
        val proficiencyLevels = mutableMapOf<String, Int>()
        baseInfo.staticProficiencies.forEach {
            val normalizedId = normalizeProficiencyId(it.id, it.kind)
            proficiencyLevels[normalizedId] = 1
        }

        val allSelectionMaps = mapOf(
            "race" to baseInfo.raceSelections,
            "background" to baseInfo.backgroundSelections
        ) + levelStack.mapIndexed { i, ls -> "level$i" to ls.selections }.toMap()

        allSelectionMaps.entries.forEach { (_, selectionMap) ->
            selectionMap.forEach selectionLoop@{ (key, result) ->
                if (key == keyToExclude) return@selectionLoop

                val items: List<String>
                val level: Int
                val selectedKind: ProficiencyKind

                when (result) {
                    is ChoiceResult.SelectedOptions -> {
                        val kind = if (result.proficiencyKind != ProficiencyKind.NONE) {
                            result.proficiencyKind
                        } else {
                            val first = result.items.firstOrNull() ?: ""
                            when {
                                first.startsWith("skill-") -> ProficiencyKind.SKILL
                                first.startsWith("tool-") -> ProficiencyKind.TOOL
                                first.startsWith("lang-") -> ProficiencyKind.LANGUAGE
                                else -> ProficiencyKind.NONE
                            }
                        }

                        if (kind == ProficiencyKind.NONE) return@selectionLoop

                        items = result.items
                        level = result.targetProficiencyLevel
                        selectedKind = kind
                    }
                    is ChoiceResult.Skills -> {
                        items = result.skillIndexes
                        level = 1
                        selectedKind = ProficiencyKind.SKILL
                    }
                    else -> return@selectionLoop
                }

                items.forEach { id ->
                    val normalizedId = normalizeProficiencyId(id, selectedKind)
                    val currentLevel = proficiencyLevels.getOrDefault(normalizedId, 0)
                    proficiencyLevels[normalizedId] = maxOf(currentLevel, level)
                }
            }
        }
        return proficiencyLevels
    }

    private fun collectSpellChoices(results: Iterable<ChoiceResult>, accumulator: MutableSet<String>) {
        results.filterIsInstance<ChoiceResult.Spells>()
            .forEach { accumulator.addAll(it.spellIndexes) }
    }

    fun getAllLearnedSpells(): Set<String> {
        val spells = mutableSetOf<String>()
        spells.addAll(baseInfo.staticSpells)
        levelStack.forEach { spells.addAll(it.autoSpells) }

        collectSpellChoices(baseInfo.raceSelections.values + baseInfo.backgroundSelections.values, spells)
        levelStack.forEach { collectSpellChoices(it.selections.values, spells) }

        return spells
    }

    fun getHistoricalLearnedSpells(untilLevelIndex: Int): Set<String> {
        val spells = mutableSetOf<String>()
        spells.addAll(baseInfo.staticSpells)

        collectSpellChoices(baseInfo.raceSelections.values + baseInfo.backgroundSelections.values, spells)

        val safeIndex = untilLevelIndex.coerceAtLeast(0)
        levelStack.take(safeIndex).forEach { levelStep ->
            spells.addAll(levelStep.autoSpells)
            collectSpellChoices(levelStep.selections.values, spells)
        }

        return spells
    }

    fun getAllSelectedSpells(): Set<String> = getAllLearnedSpells()

    fun getHighPrioritySpellExclusions(): Set<String> = getAllLearnedSpells()

    fun getHardExclusions(): Set<String> {
        return baseInfo.staticProficiencies.map { it.id }.toSet()
    }

    fun getPickedProficiencies(): List<StaticProficiency> {
        val proficiencies = mutableMapOf<String, StaticProficiency>()
        baseInfo.staticProficiencies.forEach {
            val normalizedId = normalizeProficiencyId(it.id, it.kind)
            proficiencies[normalizedId] = StaticProficiency(normalizedId, it.kind)
        }

        fun processSelections(selections: Map<String, ChoiceResult>) {
            selections.values.forEach { result ->
                if (result is ChoiceResult.SelectedOptions && result.proficiencyKind != ProficiencyKind.NONE) {
                    result.items.forEach {
                        val normalizedId = normalizeProficiencyId(it, result.proficiencyKind)
                        val normalizedKind = when {
                            normalizedId.startsWith("tool-") -> ProficiencyKind.TOOL
                            normalizedId.startsWith("skill-") -> ProficiencyKind.SKILL
                            normalizedId.startsWith("lang-") -> ProficiencyKind.LANGUAGE
                            else -> result.proficiencyKind
                        }
                        proficiencies[normalizedId] = StaticProficiency(normalizedId, normalizedKind)
                    }
                } else if (result is ChoiceResult.Skills) {
                    result.skillIndexes.forEach {
                        proficiencies[it] = StaticProficiency(it, ProficiencyKind.SKILL)
                    }
                }
            }
        }

        processSelections(baseInfo.raceSelections)
        processSelections(baseInfo.backgroundSelections)
        levelStack.forEach { step -> processSelections(step.selections) }

        return proficiencies.values.toList()
    }

    private fun normalizeProficiencyId(id: String, kind: ProficiencyKind): String {
        val clean = id.trim()
        if (clean.startsWith("tool-") || clean.startsWith("skill-") || clean.startsWith("lang-")) {
            if (!clean.startsWith("skill-")) return clean
            val token = clean.removePrefix("skill-")
            return if (token.contains("tool") || token.endsWith("-tools") || token.endsWith("-set")) {
                "tool-$token"
            } else {
                clean
            }
        }
        return when (kind) {
            ProficiencyKind.TOOL -> "tool-$clean"
            ProficiencyKind.SKILL -> "skill-$clean"
            ProficiencyKind.LANGUAGE -> "lang-$clean"
            else -> clean
        }
    }

    fun resolveEffectiveStats(): Map<String, Int> {
        return baseInfo.baseAbilityScores.mapValues { (stat, value) ->
            value + (baseInfo.aggregateStatBonuses[stat] ?: 0)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\DraftCharacter.kt
