// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\MagicRegistryAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MagicRegistryAssembler @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val calculator: DndCalculator,
    private val json: Json
) {
    private data class SpellOverride(
        val forceRitual: Boolean = false,
        val isAlwaysPrepared: Boolean = false,
        val isFree: Boolean = false
    )

    private data class SpellCandidate(val index: String, val priority: SpellPriority)

    private enum class SpellPriority(val rank: Int) {
        RACE(0),
        CLASS(1),
        FEATURE(2),
        ITEM(3),
        OTHER(4)
    }

    private fun MutableMap<String, SpellPriority>.putWithPriority(id: String, priority: SpellPriority) {
        val existing = this[id]
        if (existing == null || priority.rank < existing.rank) {
            this[id] = priority
        }
    }

    suspend fun assemble(
        draft: DraftCharacter,
        statRegistry: StatRegistry,
        classMetadata: Map<String, ClassEntity>,
        progression: List<ProgressionEntity>,
        inventory: List<InventoryItemSnapshot>,
        equippedIds: Set<String>,
        attunedIds: Set<String>,
        profBonus: Int,
        activeFeatures: List<Feature>
    ): MagicalRegistrySnapshot = coroutineScope {

        val activeItems = inventory.filter { it.isActive(equippedIds, attunedIds) }

        val spellCandidates = mutableListOf<SpellCandidate>().apply {
            draft.baseInfo.staticSpells.forEach { add(SpellCandidate(it, SpellPriority.RACE)) }
            draft.levelStack.forEach { step ->
                step.autoSpells.forEach { add(SpellCandidate(it, SpellPriority.CLASS)) }
                step.selections.values.filterIsInstance<ChoiceResult.Spells>().forEach {
                    it.spellIndexes.forEach { spellId -> add(SpellCandidate(spellId, SpellPriority.CLASS)) }
                }
            }
            activeItems.forEach { item ->
                item.grantedSpells.forEach { add(SpellCandidate(it, SpellPriority.ITEM)) }
            }
            collectDeepSpells(activeFeatures).forEach { add(SpellCandidate(it, SpellPriority.FEATURE)) }
        }

        val allMagicSlugs = spellCandidates
            .sortedWith(compareBy({ it.priority.rank }, { it.index }))
            .distinctBy { it.index }
            .map { it.index }

        val dbSpells = libraryRepository.getSpellsByIndexes(allMagicSlugs)
            .distinctBy { it.index }
            .associateBy { it.index }
        val latestProgressionMap = progression.groupBy { it.classIndex }.mapValues { it.value.maxByOrNull { r -> r.level } }

        val classSpellExclusions = collectDeepSpells(activeFeatures.filter { it.classIndex != null || it.subclassIndex != null }).toSet()
        val classSpellSelections = draft.levelStack.flatMap { step ->
            val choiceSpells = step.selections.values.filterIsInstance<ChoiceResult.Spells>().flatMap { it.spellIndexes }
            step.autoSpells + choiceSpells
        }.toMutableSet().apply { addAll(classSpellExclusions) }
        val totalLevel = getCharacterTotalLevel(draft)
        val raceSourceDef = async { assembleRaceSource(draft, dbSpells, statRegistry, profBonus, classSpellExclusions, totalLevel) }

        val itemSourcesDef = async { assembleItemSources(activeItems, dbSpells, statRegistry, profBonus) }
        val featureSourcesDef = async { assembleFeatureSources(activeFeatures, dbSpells, statRegistry, profBonus, totalLevel) }

        val raceSource = raceSourceDef.await()
        val itemSources = itemSourcesDef.await()
        val featureSources = featureSourcesDef.await()
        val raceFeatureSources = featureSources.filter { it.sourceType == MagicSourceType.RACE }
        val otherFeatureSources = featureSources.filter { it.sourceType != MagicSourceType.RACE }
        val raceFeatureSpells = raceFeatureSources.flatMap { it.spells }
        val combinedRaceSource = raceSource?.copy(spells = mergeRaceSpells(raceSource.spells, raceFeatureSpells))
        val fallbackRaceSources = if (combinedRaceSource == null) raceFeatureSources else emptyList()
        val raceSources = mutableListOf<MagicSourceSnapshot>().apply {
            combinedRaceSource?.let { add(it) }
            if (combinedRaceSource == null) addAll(fallbackRaceSources)
        }
        val raceSourceForIndex = combinedRaceSource ?: fallbackRaceSources.firstOrNull()
        val raceClassIndexes = raceSourceForIndex?.sourceId
            ?.removePrefix("race-")
            ?.lowercase()
            ?.let { setOf(it) } ?: emptySet()
        val raceSpellIds = raceSources.flatMap { it.spells }.map { it.id }.toMutableSet()
        val classSources = assembleDynamicClassSources(draft, classMetadata, latestProgressionMap, dbSpells, statRegistry, profBonus, activeFeatures)
        val sources = mutableListOf<MagicSourceSnapshot>().apply {
            addAll(raceSources)
            addAll(classSources)
            addAll(itemSources)
            addAll(otherFeatureSources)
        }

        val sourceTypeByIndex = sources.mapNotNull { source ->
            val normalizedId = source.sourceId.lowercase()
            val index = when {
                normalizedId.startsWith("class-") -> normalizedId.removePrefix("class-")
                normalizedId.startsWith("race-") -> normalizedId.removePrefix("race-")
                normalizedId.startsWith("item-") -> normalizedId.removePrefix("item-")
                else -> null
            }?.trim()?.takeIf { it.isNotBlank() }
            index?.let { it to source.sourceType }
        }.toMap()

        val ecl = calculator.calculateEffectiveCasterLevel(progression, raceClassIndexes, sourceTypeByIndex)
        val baseGlobalSlots = calculator.getGlobalSpellSlots(ecl)
        val fallbackGlobalSlots = extractGlobalSlotsFromProgression(progression)
        val pactMagic = calculator.calculatePactMagic(progression)
        val isPurePactCaster = calculator.isPurePactCaster(draft.levelStack, classMetadata, raceClassIndexes, sourceTypeByIndex)
        val raceSlotCounts = raceSources.flatMap { it.spells }.groupingBy { it.level }.eachCount().toMap()
        val hasRaceMagic = raceSlotCounts.isNotEmpty()
        val hasItemMagic = itemSources.isNotEmpty()
        val hasHybridMagic = !isPurePactCaster || hasRaceMagic || hasItemMagic

        val raceDualSpellIds = raceSpellIds.intersect(classSpellSelections)
        val globalSlots = if (isPurePactCaster) {
            emptyMap()
        } else if (baseGlobalSlots.isNotEmpty()) {
            baseGlobalSlots
        } else {
            fallbackGlobalSlots
        }

        MagicalRegistrySnapshot(sources, globalSlots, raceSlotCounts, pactMagic, hasHybridMagic, raceDualSpellIds)
    }

    private fun extractGlobalSlotsFromProgression(progression: List<ProgressionEntity>): Map<Int, Int> {
        if (progression.isEmpty()) return emptyMap()
        val latestByClass = progression
            .groupBy { it.classIndex }
            .mapValues { (_, rows) -> rows.maxByOrNull { it.level } }
            .values
            .filterNotNull()

        val maxByLevel = mutableMapOf<Int, Int>()
        latestByClass.forEach { row ->
            val slotsObj = row.spellcastingJson
                ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
                ?: return@forEach
            for (lvl in 1..9) {
                val key = "spell_slots_level_$lvl"
                val count = slotsObj[key]?.jsonPrimitive?.intOrNull ?: continue
                if (count > 0) {
                    val current = maxByLevel[lvl] ?: 0
                    if (count > current) maxByLevel[lvl] = count
                }
            }
        }
        return maxByLevel.toSortedMap()
    }


    private fun collectDeepSpells(features: List<Feature>): List<String> {
        val found = mutableListOf<String>()
        features.forEach { feature ->
            feature.embeddedSpells.forEach { spell -> found.add(spell.index) }
            val raw = feature.referenceJson ?: return@forEach
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject

                (root["spell_show_json"] as? JsonArray)?.forEach { el ->
                    el.jsonPrimitive.contentOrNull?.let { found.add(it) }
                }


                val mechElement = root["mechanics"]
                when (mechElement) {
                    is JsonArray -> {
                        mechElement.forEach { el ->
                            if (el is JsonObject) extractSpellIdFromObject(el, found)
                        }
                    }
                    is JsonObject -> {
                        extractSpellIdFromObject(mechElement, found)
                    }
                    else -> {}
                }


                (root["sub_actions_json"] as? JsonArray)?.forEach { el ->
                    if (el is JsonObject) {
                        el["spell_id"]?.jsonPrimitive?.content?.let { found.add(it) }
                    }
                }
            }
        }
        return found
    }

    private fun collectSpellOverrides(features: List<Feature>): Map<String, SpellOverride> {
        val overrides = mutableMapOf<String, SpellOverride>()
        features.forEach { feature ->
            val raw = feature.referenceJson ?: return@forEach
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject
                val spellOverrides = root["spell_overrides"] as? JsonObject ?: return@runCatching
                spellOverrides.forEach spellLoop@{ (spellId, dataEl) ->
                    val dataObj = dataEl as? JsonObject ?: return@spellLoop
                    val forceRitual = dataObj["force_ritual"]?.jsonPrimitive?.booleanOrNull == true
                    val isAlwaysPrepared = dataObj["is_always_prepared"]?.jsonPrimitive?.booleanOrNull == true
                    val isFree = dataObj["is_free"]?.jsonPrimitive?.booleanOrNull == true
                    if (!forceRitual && !isAlwaysPrepared && !isFree) return@spellLoop
                    val existing = overrides[spellId]
                    overrides[spellId] = if (existing == null) {
                        SpellOverride(forceRitual, isAlwaysPrepared, isFree)
                    } else {
                        SpellOverride(
                            forceRitual = existing.forceRitual || forceRitual,
                            isAlwaysPrepared = existing.isAlwaysPrepared || isAlwaysPrepared,
                            isFree = existing.isFree || isFree
                        )
                    }
                }
            }
        }
        return overrides
    }

    private fun assembleFeatureSources(
        features: List<Feature>,
        dbSpells: Map<String, Spell>,
        statRegistry: StatRegistry,
        profBonus: Int,
        totalLevel: Int
    ): List<MagicSourceSnapshot> {
        val statCode = "CHA"
        val statMod = statRegistry.modifiers[statCode] ?: 0
        val spellOverrides = collectSpellOverrides(features)

        return features
            .filter { it.embeddedSpells.isNotEmpty() && it.classIndex == null && it.subclassIndex == null }
            .mapNotNull { feature ->
                val isRaceFeature = feature.raceIndex != null || feature.subraceIndex != null
                val sourceId = if (isRaceFeature) "race-${feature.index}" else "feature-${feature.index}"
                val sourceType = if (isRaceFeature) MagicSourceType.RACE else MagicSourceType.CLASS
                val spellSnapshots = feature.embeddedSpells.mapNotNull { s ->
                    dbSpells[s.index]?.takeIf { spell -> !isRaceFeature || isRaceSpellUnlocked(spell.level, totalLevel) }
                        ?.let { mapToSpellSnapshot(it, sourceId, true, spellOverrides[s.index]) }
                }

                if (spellSnapshots.isEmpty()) return@mapNotNull null

                MagicSourceSnapshot(
                    sourceId = sourceId,
                    displayName = feature.name,
                    sourceType = sourceType,
                    preparationMode = PreparationMode.KNOWN,
                    saveDc = 8 + statMod + profBonus,
                    attackBonus = statMod + profBonus,
                    castingStatCode = statCode,
                    maxPreparedSpells = 0,
                    spells = spellSnapshots.sortedWith(compareBy({ it.level }, { it.name }))
                )
            }
    }

    private fun extractSpellIdFromObject(obj: JsonObject, list: MutableList<String>) {
        val type = obj["type"]?.jsonPrimitive?.content
        if (type == "ADD_ACTION") {
            obj["spell_id"]?.jsonPrimitive?.content?.let { list.add(it) }
        }

        obj["synergy"]?.jsonObject?.let { syn ->
            if (syn["type"]?.jsonPrimitive?.content == "ADD_ACTION") {
                 syn["spell_id"]?.jsonPrimitive?.content?.let { list.add(it) }
            }
        }
    }

    private suspend fun assembleDynamicClassSources(
        draft: DraftCharacter,
        classMetadata: Map<String, ClassEntity>,
        latestProgressionMap: Map<String, ProgressionEntity?>,
        dbSpells: Map<String, Spell>,
        statRegistry: StatRegistry,
        profBonus: Int,
        activeFeatures: List<Feature>
    ): List<MagicSourceSnapshot> {
        return draft.levelStack.groupBy { it.classIndex }.mapNotNull outer@ { (classIdx, steps) ->
            val metadata = classMetadata[classIdx] ?: return@outer null
            val statCode = metadata.primaryStat ?: "CHA"
            val statMod = statRegistry.modifiers[statCode] ?: 0

            val autoSpells = steps.flatMap { it.autoSpells }.toSet()
            val chosenSpells = steps.flatMap { it.selections.values }
                .filterIsInstance<ChoiceResult.Spells>().flatMap { it.spellIndexes }.toSet()


            val relevantFeatures = activeFeatures.filter {
                it.classIndex == classIdx ||
                (it.subclassIndex != null && steps.any { step -> step.subclassIndex == it.subclassIndex })
            }
            val deepSpells = collectDeepSpells(relevantFeatures).toSet()
            val spellOverrides = collectSpellOverrides(relevantFeatures)

            val latestRow = latestProgressionMap[classIdx]
            val basePreparationMode = calculator.getPreparationMode(metadata.casterType)
            val progressionSuggestsPrepared = latestRow?.prepFormulaType?.uppercase() in setOf("FULL", "HALF")
            val featureSuggestsPrepared = relevantFeatures.any { feature ->
                if (!(feature.index.startsWith("spellcasting-") || feature.uiGroup == "SPELLS")) return@any false
                val raw = feature.referenceJson ?: return@any false
                runCatching {
                    val root = json.parseToJsonElement(raw).jsonObject
                    root["casting_type"]?.jsonPrimitive?.content?.equals("PREPARED", ignoreCase = true) == true
                }.getOrDefault(false)
            }
            val preparationMode = when {
                basePreparationMode == PreparationMode.PREPARED -> PreparationMode.PREPARED
                basePreparationMode == PreparationMode.NONE && progressionSuggestsPrepared &&
                    !classIdx.equals("warlock", ignoreCase = true) -> PreparationMode.PREPARED
                basePreparationMode == PreparationMode.NONE && featureSuggestsPrepared -> PreparationMode.PREPARED
                else -> basePreparationMode
            }
            val maxSpellLevel = calculator.getMaxSpellLevel(classIdx, steps.size)
            val includeFullPreparedPool = classIdx.lowercase() != "wizard"
            val preparedPoolById = if (preparationMode == PreparationMode.PREPARED && includeFullPreparedPool) {
                libraryRepository.getAllSpellsByClass(classIdx)
                    .asSequence()
                    .filter { it.level in 1..maxSpellLevel }
                    .associateBy { it.index }
            } else {
                emptyMap()
            }
            val classPoolIds = preparedPoolById.keys
            val allClassSpellIds = autoSpells + chosenSpells + deepSpells + classPoolIds

            val spellSnapshots = allClassSpellIds.mapNotNull inner@ { id ->
                val spell = dbSpells[id] ?: preparedPoolById[id]
                spell?.let { s ->
                    val isAlwaysPrepared = id in autoSpells || id in deepSpells
                    mapToSpellSnapshot(s, "class-$classIdx", isAlwaysPrepared, spellOverrides[id])
                }
            }

            if (spellSnapshots.isEmpty() && metadata.casterType == null) return@outer null

            val limit = calculator.calculateMaxPrepared(latestRow?.prepFormulaType, steps.size, statMod)

            MagicSourceSnapshot(
                sourceId = "class-$classIdx",
                displayName = metadata.name,
                sourceType = MagicSourceType.CLASS,
                preparationMode = preparationMode,
                saveDc = 8 + statMod + profBonus,
                attackBonus = statMod + profBonus,
                castingStatCode = statCode,
                maxPreparedSpells = limit,
                spells = spellSnapshots.sortedWith(compareBy({ it.level }, { it.name }))
            )
        }
    }

    private suspend fun assembleRaceSource(
        draft: DraftCharacter,
        dbSpells: Map<String, Spell>,
        statRegistry: StatRegistry,
        profBonus: Int,
        excludedSpellIds: Set<String>,
        totalLevel: Int
    ): MagicSourceSnapshot? {
        val raceIdx = draft.baseInfo.raceIndex.takeIf { it.isNotBlank() } ?: return null
        val raceName = libraryRepository.getRaceByIndex(raceIdx)?.name ?: "Race"

        val statMod = statRegistry.modifiers["CHA"] ?: 0
        val spells = draft.baseInfo.staticSpells
            .filterNot { it in excludedSpellIds }
            .mapNotNull { spellId ->
                dbSpells[spellId]
                    ?.takeIf { spell -> isRaceSpellUnlocked(spell.level, totalLevel) }
                    ?.let { spell -> mapToSpellSnapshot(spell, "race-$raceIdx", true) }
            }

        return if (spells.isEmpty()) null else MagicSourceSnapshot(
            sourceId = "race-$raceIdx",
            displayName = raceName,
            sourceType = MagicSourceType.RACE,
            preparationMode = PreparationMode.KNOWN,
            saveDc = 8 + statMod + profBonus,
            attackBonus = statMod + profBonus,
            castingStatCode = "CHA",
            maxPreparedSpells = 0,
            spells = spells
        )
    }

    private fun assembleItemSources(
        activeItems: List<InventoryItemSnapshot>,
        dbSpells: Map<String, Spell>,
        statRegistry: StatRegistry,
        profBonus: Int
    ): List<MagicSourceSnapshot> {
        return activeItems.mapNotNull { item ->
            val spells = item.grantedSpells.mapNotNull { dbSpells[it]?.let { s -> mapToSpellSnapshot(s, "item-${item.uniqueId}", true) } }

            if (spells.isEmpty() && item.maxCharges == 0) return@mapNotNull null

            val statCode = item.scalingStat ?: "INT"
            val statMod = statRegistry.modifiers[statCode] ?: 0

            MagicSourceSnapshot(
                sourceId = "item-${item.uniqueId}",
                displayName = item.name,
                sourceType = MagicSourceType.ITEM,
                preparationMode = PreparationMode.NONE,
                saveDc = 8 + statMod + profBonus + item.magicBonusSaveDc,
                attackBonus = statMod + profBonus + item.magicBonusAttack,
                castingStatCode = statCode,
                maxPreparedSpells = 0,
                spells = spells,
                exclusiveResourcePoolId = item.poolId,
                resetRule = item.resetRule
            )
        }
    }

    private fun mapToSpellSnapshot(s: Spell, sourceId: String, isAlways: Boolean, override: SpellOverride? = null): SpellSnapshot {
        val fallbackDice = s.damageMap[1] ?: s.damageMap.values.firstOrNull()
        val isRitual = if (override?.forceRitual == true) true else s.isRitual
        val isAlwaysPrepared = isAlways || override?.isAlwaysPrepared == true || s.level == 0
        val isFreeCast = override?.isFree == true

        return SpellSnapshot(
            uniqueId = "${sourceId}_${s.index}",
            id = s.index,
            name = s.name.stripHtml(),
            level = s.level,
            school = s.school,
            time = s.castingTime,
            range = s.range,
            components = s.components,
            duration = s.duration,
            description = s.description.stripHtml(),
            isRitual = isRitual,
            isConcentration = s.isConcentration,
            isAlwaysPrepared = isAlwaysPrepared,
            attackType = s.attackType,
            damageDice = fallbackDice,
            damageMap = s.damageMap,
            damageType = s.damageType,
            saveStat = s.saveStat,
            isFreeCast = isFreeCast
        )
    }

    private fun getCharacterTotalLevel(draft: DraftCharacter): Int =
        draft.levelStack.size.coerceAtLeast(1)

    private fun isRaceSpellUnlocked(spellLevel: Int, totalLevel: Int): Boolean {
        val requiredLevel = when (spellLevel) {
            0 -> 1
            1 -> 3
            2 -> 5
            else -> Int.MAX_VALUE
        }
        return totalLevel >= requiredLevel
    }

    private fun mergeRaceSpells(primary: List<SpellSnapshot>, additions: List<SpellSnapshot>): List<SpellSnapshot> {
        return (primary + additions)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.level }, { it.name }))
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\MagicRegistryAssembler.kt
