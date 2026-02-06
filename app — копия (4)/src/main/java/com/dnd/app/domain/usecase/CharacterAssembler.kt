// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterAssembler @Inject constructor(
    private val repository: LibraryRepository,
    private val calculator: DndCalculator
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun assemble(draft: DraftCharacter): CharacterDomain {
        val finalStatsMap = mutableMapOf<String, Int>()
        val skillProficiencies = mutableMapOf<String, Int>()
        val autoLearnedSpells = mutableListOf<String>()
        val features = mutableListOf<Feature>()
        val allEquipmentIndexes = mutableListOf<String>()

        var baseSpeed = 30
        var extraHp = 0

        val allSelections = mutableMapOf<String, ChoiceResult>()
        allSelections.putAll(draft.baseInfo.raceSelections)
        allSelections.putAll(draft.baseInfo.backgroundSelections)
        draft.levelStack.forEach { allSelections.putAll(it.selections) }

        processBaseProficiencies(allSelections, skillProficiencies, autoLearnedSpells)
        processExpertise(allSelections, skillProficiencies)

        draft.baseInfo.baseAbilityScores.forEach { (stat, baseValue) ->
            val totalBonus = draft.baseInfo.aggregateStatBonuses[stat] ?: 0
            finalStatsMap[stat] = baseValue + totalBonus
        }

        val race = repository.getRaceByIndex(draft.baseInfo.raceIndex)
        if (race != null) {
            baseSpeed = race.speed
            val baseRaceFeatures = repository.getBaseRaceFeatures(race.id)
            val subraceFeatures = draft.baseInfo.subraceIndex?.let { repository.getSubraceFeatures(it) } ?: emptyList()
            val allRaceFeatures = baseRaceFeatures + subraceFeatures
            features.addAll(allRaceFeatures)
            race.baseProficiencies.forEach { skillProficiencies[it] = 1 }
        }

        val firstLevelClassIndex = draft.levelStack.firstOrNull()?.classIndex
        if (firstLevelClassIndex != null) {
            val classEntity = repository.getClassEntityByIndex(firstLevelClassIndex)
            classEntity?.proficienciesJson?.let { rawJson ->
                try {
                    json.decodeFromString<List<ReferenceJson>>(rawJson).forEach { skillProficiencies[it.index] = 1 }
                } catch (e: Exception) { /* ignore */ }
            }
            classEntity?.savingThrowsJson?.let { rawJson ->
                try {
                    json.decodeFromString<List<ReferenceJson>>(rawJson).forEach { skillProficiencies["saving-throw-${it.index.lowercase()}"] = 1 }
                } catch (e: Exception) { /* ignore */ }
            }
        }

        allEquipmentIndexes.addAll(draft.baseInfo.staticEquipment)
        draft.baseInfo.inventorySelections.values.forEach { result ->
            if (result is ChoiceResult.SelectedOptions) {
                allEquipmentIndexes.addAll(result.items)
            }
        }

        val unpackedEquipment = unpackEquipmentBundles(allEquipmentIndexes.distinct())
        val finalInventoryIds = repository.getEquipmentIdsByNames(unpackedEquipment)

        var hpMax = extraHp
        val conMod = calculator.calculateModifier(finalStatsMap["CON"] ?: 10)

        draft.levelStack.forEachIndexed { index, step ->
            val lvl = index + 1
            val classInfo = repository.getAllClasses().find { it.index == step.classIndex }
            if (index == 0) hpMax += (classInfo?.hitDie ?: 8) + conMod
            else hpMax += step.hpIncrease + conMod

            val featuresForLevel = repository.getClassFeaturesForLevel(step.classIndex, lvl, step.subclassIndex)
            features.addAll(featuresForLevel.baseClassFeatures)
            featuresForLevel.subclassChoiceFeature?.let { features.add(it) }
            features.addAll(featuresForLevel.selectedSubclassFeatures)
        }

        val classString = draft.levelStack.groupBy { it.classIndex }
            .map { (idx, list) -> "${DndLocalization.translateSkill(idx)} ${list.size}" }
            .joinToString(" / ")

        return CharacterDomain(
            id = draft.id,
            name = draft.name.ifBlank { "Герой" },
            raceName = race?.name ?: "",
            className = classString,
            level = draft.levelStack.size.coerceAtLeast(1),
            stats = Stats(
                strength = finalStatsMap["STR"] ?: 10,
                dexterity = finalStatsMap["DEX"] ?: 10,
                constitution = finalStatsMap["CON"] ?: 10,
                intelligence = finalStatsMap["INT"] ?: 10,
                wisdom = finalStatsMap["WIS"] ?: 10,
                charisma = finalStatsMap["CHA"] ?: 10
            ),
            hpMax = hpMax, hpCurrent = hpMax,
            speed = baseSpeed,
            features = features.distinctBy { it.index },
            inventoryIds = finalInventoryIds,
            raceSpellIds = autoLearnedSpells.distinct(),
            skillProficiencies = skillProficiencies,
            bio = Bio(
                alignment = draft.baseInfo.alignmentIndex,
                background = draft.baseInfo.backgroundIndex,
                backgroundName = repository.getAllBackgrounds().find { it.name == draft.baseInfo.backgroundIndex }?.name ?: draft.baseInfo.backgroundIndex,
                traits = draft.baseInfo.personalityTrait,
                ideals = draft.baseInfo.ideal,
                bonds = draft.baseInfo.bond,
                flaws = draft.baseInfo.flaw
            )
        )
    }

    private suspend fun unpackEquipmentBundles(initialIndexes: List<String>): List<String> {
        val finalIndexes = mutableListOf<String>()
        val processingQueue = ArrayDeque(initialIndexes)
        val seenBundles = mutableSetOf<String>()

        while (processingQueue.isNotEmpty()) {
            val currentIndex = processingQueue.removeFirst()
            val entity = repository.getEquipmentByIndexes(listOf(currentIndex)).firstOrNull()

            if (entity != null && (entity.indexName.startsWith("bundle-") || !entity.contentsJson.isNullOrBlank())) {
                if (seenBundles.add(entity.indexName)) { // Предотвращение бесконечных циклов
                    try {
                        entity.contentsJson?.let { rawJson ->
                            val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                            for (item in contents) {
                                item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.let { index ->
                                    val count = item["quantity"]?.jsonPrimitive?.int ?: 1
                                    repeat(count) { processingQueue.addLast(index) }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ASSEMBLER_UNPACK", "Failed to parse contents_json for ${entity.indexName}", e)
                        finalIndexes.add(entity.indexName) // Fallback
                    }
                }
            } else {
                finalIndexes.add(currentIndex)
            }
        }
        return finalIndexes
    }

    private fun processBaseProficiencies(
        results: Map<String, ChoiceResult>,
        skills: MutableMap<String, Int>,
        spells: MutableList<String>
    ) {
        results.filter { !it.key.contains("expertise", true) }.forEach { (_, result) ->
            when (result) {
                is ChoiceResult.Skills -> result.skillIndexes.forEach { skills[it] = 1 }
                is ChoiceResult.Spells -> spells.addAll(result.spellIndexes)
                is ChoiceResult.SelectedOptions -> result.items.forEach { id ->
                    if (id.startsWith("skill-") || id.startsWith("tool-") || id.startsWith("saving-throw-")) {
                        skills[id] = 1
                    }
                }
                else -> {}
            }
        }
    }

    private fun processExpertise(
        results: Map<String, ChoiceResult>,
        skills: MutableMap<String, Int>
    ) {
        results.filter { it.key.contains("expertise", true) }.forEach { (_, result) ->
            val skillIds = when (result) {
                is ChoiceResult.Skills -> result.skillIndexes
                is ChoiceResult.SelectedOptions -> result.items.filter { it.startsWith("skill-") || it.startsWith("tool-") }
                else -> emptyList()
            }
            skillIds.forEach { id ->
                if (skills.containsKey(id)) {
                    skills[id] = 2
                }
            }
        }
        if (results.keys.any { it.contains("rogue-expertise") } &&
            (results.values.firstOrNull { it is ChoiceResult.SelectedOptions } as? ChoiceResult.SelectedOptions)?.items?.any { it.contains("multiple-bundle") } == true) {
            skills["tool-thieves-tools"] = 2
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt