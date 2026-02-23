// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\CharacterAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.DndLocalization
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterAssembler @Inject constructor(
    private val repository: LibraryRepository,
    private val calculator: DndCalculator
) {
    private val json = Json { ignoreUnknownKeys = true }


    suspend fun assemble(draft: DraftCharacter): CharacterDomain {
        val allEquipmentCategoryIndexes = mutableSetOf<String>()
        val rootCategories = repository.getRootShopCategories()
        val queue = ArrayDeque(rootCategories)
        while(queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (allEquipmentCategoryIndexes.add(current.index)) {
                val children = repository.getChildShopCategories(current.index)
                queue.addAll(children)
            }
        }

        val finalStatsMap = mutableMapOf<String, Int>()
        val skillProficiencies = mutableMapOf<String, Int>()
        val autoLearnedSpells = mutableListOf<String>()
        val features = mutableListOf<Feature>()
        val allEquipmentIndexes = mutableListOf<String>()

        var baseSpeed = 30
        var hpMax = 0


        var totalGold = draft.baseInfo.startingGold

        draft.baseInfo.staticProficiencies.forEach { skillProficiencies[it.id] = 1 }

        val allSelections = draft.baseInfo.raceSelections.values +
                draft.baseInfo.backgroundSelections.values +
                draft.levelStack.flatMap { it.selections.values } +
                draft.baseInfo.inventorySelections.values

        processSelections(allSelections, skillProficiencies, autoLearnedSpells)

        draft.baseInfo.baseAbilityScores.forEach { (stat, baseValue) ->
            val totalBonus = draft.baseInfo.aggregateStatBonuses[stat] ?: 0
            finalStatsMap[stat] = baseValue + totalBonus
        }

        val race = repository.getRaceByIndex(draft.baseInfo.raceIndex)
        if (race != null) {
            baseSpeed = race.speed
            features.addAll(repository.getBaseRaceFeatures(race.id))
            draft.baseInfo.subraceIndex?.let { features.addAll(repository.getSubraceFeatures(it)) }
        }

        val background = repository.getBackgroundByIndex(draft.baseInfo.backgroundIndex)
        if (background != null) {
            features.addAll(background.features)

        }

        val conMod = calculator.calculateModifier(finalStatsMap["CON"] ?: 10)
        draft.levelStack.forEachIndexed { index, step ->
            val lvl = index + 1
            val classInfo = repository.getAllClasses().find { it.index == step.classIndex }
            hpMax += (if (index == 0) (classInfo?.hitDie ?: 8) else step.hpIncrease) + conMod
            val featuresForLevel = repository.getClassFeaturesForLevel(step.classIndex, lvl, step.subclassIndex)
            features.addAll(featuresForLevel.baseClassFeatures)
            featuresForLevel.subclassChoiceFeature?.let { features.add(it) }
            features.addAll(featuresForLevel.selectedSubclassFeatures)
        }

        allEquipmentIndexes.addAll(draft.baseInfo.staticEquipment)
        allSelections.filterIsInstance<ChoiceResult.SelectedOptions>().forEach {
            if (it.proficiencyKind == ProficiencyKind.NONE) {
                allEquipmentIndexes.addAll(it.items.filter { itemIndex -> itemIndex !in allEquipmentCategoryIndexes })
            }
        }
        val inventoryIds = repository.getEquipmentIdsByNames(unpackEquipmentBundles(allEquipmentIndexes.distinct()))

        val allSpellsDb = repository.getAllSpells().first()
        val allDraftAutoSpells = draft.baseInfo.staticSpells + draft.levelStack.flatMap { it.autoSpells }
        val finalSpellIds = allSpellsDb.filter { it.index in allDraftAutoSpells || it.index in autoLearnedSpells }.map { it.id }.distinct()

        val localizedProficiencies = skillProficiencies.entries.associate { (id, level) ->
            DndLocalization.translateProficiency(id) to level
        }

        val langMap = repository.getAllLanguages().associateBy { "lang-${it.indexName}" }
        val languageNames = skillProficiencies.keys.filter { it.startsWith("lang-") }.mapNotNull { langMap[it]?.name }

        val classLabel = draft.levelStack.groupBy { it.classIndex }.map { (idx, list) -> "${DndLocalization.translateProficiency(idx)} ${list.size}" }.joinToString(" / ")

        return CharacterDomain(
            id = draft.id, name = draft.name.ifBlank { "Герой" },
            raceName = race?.name ?: "", className = classLabel, level = draft.levelStack.size.coerceAtLeast(1),
            stats = Stats(
                strength = finalStatsMap["STR"] ?: 10, dexterity = finalStatsMap["DEX"] ?: 10, constitution = finalStatsMap["CON"] ?: 10,
                intelligence = finalStatsMap["INT"] ?: 10, wisdom = finalStatsMap["WIS"] ?: 10, charisma = finalStatsMap["CHA"] ?: 10,
                gold = totalGold
            ),
            hpMax = hpMax, hpCurrent = hpMax, speed = baseSpeed,
            features = features.distinctBy { it.index },
            inventoryIds = inventoryIds,
            spellsKnownIds = finalSpellIds,
            skillProficiencies = localizedProficiencies,
            languages = languageNames,
            bio = Bio(
                alignment = draft.baseInfo.alignmentIndex, background = draft.baseInfo.backgroundIndex,
                backgroundName = background?.name ?: draft.baseInfo.backgroundIndex,
                traits = draft.baseInfo.personalityTrait, ideals = draft.baseInfo.ideal,
                bonds = draft.baseInfo.bond, flaws = draft.baseInfo.flaw
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
                if (seenBundles.add(entity.indexName)) {
                    try {
                        entity.contentsJson?.let { rawJson ->
                            json.decodeFromString<List<JsonObject>>(rawJson).forEach { item ->
                                val index = item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                                if (index != null) {
                                    val count = item["quantity"]?.jsonPrimitive?.int ?: 1
                                    repeat(count) { processingQueue.addLast(index) }
                                }
                            }
                        }
                    } catch (e: Exception) { finalIndexes.add(entity.indexName) }
                }
            } else finalIndexes.add(currentIndex)
        }
        return finalIndexes
    }

    private fun processSelections(results: Collection<ChoiceResult>, skills: MutableMap<String, Int>, spells: MutableList<String>) {
        results.forEach { result ->
            when (result) {
                is ChoiceResult.Spells -> spells.addAll(result.spellIndexes)
                is ChoiceResult.SelectedOptions -> {
                    if (result.proficiencyKind != ProficiencyKind.NONE) {
                        result.items.forEach { id ->
                            skills[id] = skills.getOrDefault(id, 0).coerceAtLeast(result.targetProficiencyLevel)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\CharacterAssembler.kt