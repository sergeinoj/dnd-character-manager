// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\SnapshotAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import android.util.Log
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import com.dnd.app.domain.usecase.inventory.UnpackedItem
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SnapshotAssembler @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val classFeatureRepository: ClassFeatureRepository,
    private val calculator: DndCalculator,
    private val coreStatAssembler: CoreStatAssembler,
    private val skillAssembler: SkillRegistryAssembler,
    private val featureRegistryAssembler: FeatureRegistryAssembler,
    private val resolveGlobalLoreUseCase: ResolveGlobalLoreUseCase,
    private val vitalsAssembler: VitalsAssembler,
    private val magicRegistryAssembler: MagicRegistryAssembler,
    private val resourceRegistryAssembler: ResourceRegistryAssembler,
    private val inventoryReconciler: InventoryReconciler,
    private val monstersDataSource: com.dnd.app.data.repository.datasource.MonstersDataSource,
    private val aresAssembler: AresAssembler,
    private val armorClassUseCase: CalculateArmorClassUseCase,
    private val modifierExtractor: InventoryModifierExtractor,
    private val calculateWeightUseCase: CalculateWeightUseCase,
    private val transformationApplier: TransformationApplier,
    private val json: Json
) {
    private val TAG = "ResonanceAssembler"

    suspend operator fun invoke(
        draft: DraftCharacter,
        oldSnapshot: CharacterSnapshot?,
        oldLiveState: CharacterLiveState?,
        entityCurrentHp: Int?,
        unpackedInventory: List<UnpackedItem>
    ): Pair<CharacterSnapshot, CharacterLiveState> = withContext(Dispatchers.Default) {
        try {
            assembleFullInternal(draft, oldSnapshot, oldLiveState, entityCurrentHp, unpackedInventory)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Critical assembly failure.", e)
            createDegradedResult(draft, oldSnapshot, oldLiveState, e.message)
        }
    }

    private suspend fun assembleFullInternal(
        draft: DraftCharacter,
        oldSnapshot: CharacterSnapshot?,
        oldLiveState: CharacterLiveState?,
        entityCurrentHp: Int?,
        unpackedInventory: List<UnpackedItem>
    ): Pair<CharacterSnapshot, CharacterLiveState> = coroutineScope {


        val allSkillsDef = async { libraryRepository.getAllSkills() }
        val classMetadataDef = async { fetchClassMetadata(draft) }
        val raceEntityDef = async { libraryRepository.getRaceByIndex(draft.baseInfo.raceIndex) }
        val subraceEntityDef = async {
            draft.baseInfo.subraceIndex
                ?.takeIf { it.isNotBlank() }
                ?.let { libraryRepository.getSubraceModelByIndex(it) }
        }
        val backgroundDef = async {
            if (draft.baseInfo.backgroundIndex.isNotBlank()) libraryRepository.getBackgroundByIndex(draft.baseInfo.backgroundIndex)
            else null
        }

        val damageTypeMapDef = async {
            listOf("acid", "bludgeoning", "cold", "fire", "force", "lightning", "necrotic", "piercing", "poison", "psychic", "radiant", "slashing", "thunder")
                .mapNotNull { idx -> classFeatureRepository.getDamageTypeByIndex(idx)?.let { it.indexName to it.name } }.toMap()
        }
        val languagesDef = async { libraryRepository.getAllLanguages() }

        val classMetadata = classMetadataDef.await()
        val raceEntity = raceEntityDef.await()
        val subraceEntity = subraceEntityDef.await()
        val background = backgroundDef.await()
        val globalLore = resolveGlobalLoreUseCase(draft, raceEntity)
        val damageTypeMap = damageTypeMapDef.await()
        val raceFallbackLabel = raceEntity?.name
            ?: draft.baseInfo.raceIndex.takeIf { it.isNotBlank() }
            ?: ""
        val classFallbackLabel = vitalsClassFallback(draft)


        val invResult: InventoryReconcileResult = inventoryReconciler.reconcile(unpackedInventory, oldSnapshot, oldLiveState ?: CharacterLiveState(), emptyMap())
        val actualItemIds = invResult.items.map { it.uniqueId }.toSet()
        val validEquippedIds = (oldLiveState?.equippedItemIds ?: emptySet()).intersect(actualItemIds)
        val validAttunedIds = (oldLiveState?.attunedItemIds ?: emptySet()).intersect(actualItemIds)

        val modifierRegistry = modifierExtractor.extract(invResult.items, validEquippedIds, validAttunedIds)


        val statRegistry = coreStatAssembler.assemble(draft, modifierRegistry)


        val weightReport = calculateWeightUseCase(
            inventory = invResult.items,
            itemOverrides = oldLiveState?.itemOverrides ?: emptyMap(),
            strengthScore = statRegistry.scores["STR"] ?: 10
        )


        val totalLevel = draft.levelStack.size.coerceAtLeast(1)
        val baseProfBonus = calculator.calculateProficiencyBonus(totalLevel)
        val finalProfBonus = baseProfBonus + modifierRegistry.profBonusMod
        val allProficiencies = draft.getAllProficienciesWithLevels()

        val featureResult = featureRegistryAssembler.assemble(draft, statRegistry, classMetadata, allProficiencies, raceEntity, background)

        val progressionData = fetchProgressionHistory(draft)
        val latestProgressionMap = progressionData.groupBy { it.classIndex }
            .mapValues { (_, rows) ->
                rows
                    .filter { r ->
                        (r.maxCharges > 0 && !r.resourceName.isNullOrBlank()) ||
                            (r.maxCharges2 > 0 && !r.resourceName2.isNullOrBlank())
                    }
                    .maxByOrNull { r -> r.level }
                    ?: rows.maxByOrNull { r -> r.level }
            }
        val classLevels = draft.levelStack.groupBy { it.classIndex }.mapValues { it.value.size }

        val vitals = vitalsAssembler.assemble(
            draft = draft,
            statModifiers = statRegistry.modifiers,
            classMetadata = classMetadata,
            race = raceEntity,
            weightReport = weightReport,
            oldLiveState = oldLiveState,
            oldSnapshot = oldSnapshot,
            entityCurrentHp = entityCurrentHp,
            featureHpBonus = featureResult.totalHpBonus,
            progressionRows = progressionData,
            activeFeatures = featureResult.domainFeatures,
            inventory = invResult.items,
            equippedIds = validEquippedIds,
            exhaustionLevel = oldLiveState?.exhaustionLevel ?: 0
        )

        val skills = skillAssembler.assemble(allSkillsDef.await(), statRegistry, finalProfBonus, allProficiencies, modifierRegistry)
        val (transformedStatRegistry, transformedSkills) = applyTransformationIfNeeded(statRegistry, skills, oldLiveState)

        val isPurePactCaster = calculator.isPurePactCaster(draft.levelStack, classMetadata)
        val magicSnapshot = magicRegistryAssembler.assemble(
            draft, statRegistry, classMetadata, progressionData, invResult.items, validEquippedIds, validAttunedIds, finalProfBonus,
            activeFeatures = featureResult.domainFeatures
        )

        val activeItems = invResult.items.filter { it.isActive(validEquippedIds, validAttunedIds) }
        val resourcePools = resourceRegistryAssembler.assemble(
            latestProgressionMap,
            activeItems,
            featureResult.domainFeatures,
            statRegistry.modifiers
        )

        val selectedFamiliarId = resolveSelectedFamiliarId(draft)
        val familiarRecord = selectedFamiliarId?.let { monstersDataSource.getMonster(it) }
        val transformationMonster = oldLiveState?.transformationId
            ?.takeIf { it.isNotBlank() }
            ?.let { monstersDataSource.getMonster(it) }
        if (selectedFamiliarId == null) {
            Log.d(TAG, "Familiar selection not found in draft.")
        } else if (familiarRecord == null) {
            Log.w(TAG, "Familiar selection resolved but monster not found: $selectedFamiliarId")
        } else {
            Log.d(TAG, "Familiar selected: index=${familiarRecord.index} name=${familiarRecord.name}")
        }


        val calculatedAc = armorClassUseCase(
            invResult.items,
            validEquippedIds,
            transformedStatRegistry.modifiers,
            featureResult.domainFeatures,
            modifierRegistry.acBonus
        )
        val finalAc = transformationMonster?.armorClass ?: calculatedAc

        val combatActions = aresAssembler.assemble(
            statRegistry = statRegistry,
            inventory = invResult.items,
            magic = magicSnapshot,
            profBonus = finalProfBonus,
            proficiencies = allProficiencies,
            totalCharLevel = totalLevel,
            damageTypeMap = damageTypeMap,
            equippedIds = validEquippedIds,
            resourcePools = resourcePools,
            liveStateCharges = oldLiveState?.featureCharges ?: emptyMap(),
            innateUsage = oldLiveState?.innateUsage ?: emptyMap(),
            activeFeatures = featureResult.domainFeatures,
            progressionRows = progressionData,
            classLevels = classLevels,
            activeEffects = oldLiveState?.activeEffects ?: emptySet(),
            familiarRecord = familiarRecord
        )

        val languageMap = languagesDef.await().associate { "lang-${it.indexName}" to it.name }
        val languageKeys = draft.getAllProficienciesWithLevels().keys.filter { it.startsWith("lang-") }.toSet()
        val languageNames = languageKeys.mapNotNull { languageMap[it] }.distinct()
        val skillMap = allSkillsDef.await().associate { "skill-${it.indexName}" to it.name }
        val proficiencyMap = libraryRepository.getAllProficiencies().associate { it.indexName to it.name }
        val toolIndexes = allProficiencies.keys
            .asSequence()
            .filter { it.startsWith("tool-") }
            .map { it.removePrefix("tool-") }
            .filter { it.isNotBlank() }
            .toSet()
        val toolMap = if (toolIndexes.isNotEmpty()) {
            libraryRepository.getEquipmentByIndexes(toolIndexes.toList())
                .associate { "tool-${it.indexName}" to it.name }
        } else {
            emptyMap()
        }
        val proficiencyLabels = allProficiencies.keys
            .associateWith { id ->
                val raw = id.removePrefix("tool-").removePrefix("skill-").removePrefix("lang-")
                skillMap[id]
                    ?: languageMap[id]
                    ?: toolMap[id]
                    ?: proficiencyMap[id]
                    ?: proficiencyMap[raw]
                    ?: ""
            }
            .filterValues { it.isNotBlank() }

        val newSnapshot = CharacterSnapshot(
            versionId = (oldSnapshot?.versionId ?: 0) + 1,
            global = GlobalInfo(
                name = draft.name,
                race = raceFallbackLabel,
                subrace = subraceEntity?.name,
                raceDescription = globalLore.raceDescription,
                subraceDescription = globalLore.subraceDescription,
                classTitle = vitals.classTitle.ifBlank { classFallbackLabel },
                subclassName = globalLore.subclassName,
                subclassDescription = globalLore.subclassDescription,
                level = totalLevel,
                alignment = draft.baseInfo.alignmentIndex,
                alignmentDescription = globalLore.alignmentDescription,
                gender = draft.baseInfo.gender,
                personalityTrait = draft.baseInfo.personalityTrait,
                ideal = draft.baseInfo.ideal,
                bond = draft.baseInfo.bond,
                flaw = draft.baseInfo.flaw,
                appearance = draft.baseInfo.appearance,
                backstory = draft.baseInfo.backstory,
                backgroundName = background?.name ?: "",
                classes = vitals.classSnapshots
            ),
            stats = transformedStatRegistry.models.values.toList(),
            statsMap = transformedStatRegistry.models,
            skills = transformedSkills,
            maxHp = vitals.maxHp,
            hitDice = vitals.hitDice,
            hitDiceCount = totalLevel,
            finalArmorClass = finalAc,
            finalSpeed = vitals.finalSpeed,
            initiativeBonus = transformedStatRegistry.models["DEX"]?.modifier ?: "+0",
            totalWeight = weightReport.totalWeight,
            maxCarryWeight = weightReport.maxCarryWeight,
            isEncumbered = weightReport.isEncumbered,
            proficiencyBonus = finalProfBonus,
            features = featureResult.displayModels,
            magic = magicSnapshot,
            resourcePools = resourcePools,
          inventory = invResult.items,
          combatActions = combatActions,
          familiar = familiarRecord,
          transformationMonster = transformationMonster,
          languages = languageNames,
          proficiencies = allProficiencies,
          proficiencyLabels = proficiencyLabels,
          isPurePactCaster = isPurePactCaster,
          canWildShape = vitals.canWildShape
      )

        val preparedOnCreation = if (oldLiveState == null) {
            magicSnapshot.sources
                .filter { it.preparationMode == PreparationMode.PREPARED }
                .associate { source ->
                    val alwaysPrepared = source.spells
                        .asSequence()
                        .filter { it.isAlwaysPrepared }
                        .map { it.id }
                        .toSet()
                    val optionalPrepared = source.spells
                        .asSequence()
                        .filter { !it.isAlwaysPrepared && it.level > 0 }
                        .sortedWith(compareBy({ it.level }, { it.name }))
                        .take(source.maxPreparedSpells.coerceAtLeast(0))
                        .map { it.id }
                        .toSet()
                    source.sourceId to (alwaysPrepared + optionalPrepared)
                }
                .filterValues { it.isNotEmpty() }
        } else {
            oldLiveState.preparedSpellIds
        }

        val newLiveState = (oldLiveState ?: CharacterLiveState()).copy(
            hpCurrent = vitals.currentHp,
            hpTemp = vitals.tempHp,
            equippedItemIds = validEquippedIds,
            attunedItemIds = validAttunedIds,
            preparedSpellIds = preparedOnCreation,
            transformationId = oldLiveState?.transformationId,
            transformationHp = oldLiveState?.transformationHp ?: 0,
            concentrationSpellId = oldLiveState?.concentrationSpellId,
            activeConditions = oldLiveState?.activeConditions ?: emptySet()
        )

        newSnapshot to newLiveState
    }

    private suspend fun createDegradedResult(draft: DraftCharacter, oldS: CharacterSnapshot?, oldL: CharacterLiveState?, err: String?) = Pair(
        CharacterSnapshot(
            versionId = (oldS?.versionId ?: 0) + 1,
            global = GlobalInfo(name = draft.name, classTitle = "RECOVERY", level = draft.levelStack.size),
            notes = "ERROR: $err"
        ),
        oldL ?: CharacterLiveState()
    )

    private fun vitalsClassFallback(draft: DraftCharacter): String {
        val fromLevels = draft.levelStack
            .groupBy { it.classIndex }
            .entries
            .mapNotNull { (idx, steps) -> idx.takeIf { it.isNotBlank() }?.let { "$it ${steps.size}" } }
        if (fromLevels.isNotEmpty()) return fromLevels.joinToString(" / ")
        val starting = draft.baseInfo.startingClassIndex.takeIf { it.isNotBlank() }
        return starting?.let { "$it 1" } ?: ""
    }

    private suspend fun applyTransformationIfNeeded(
        statRegistry: StatRegistry,
        skills: List<SkillModel>,
        liveState: CharacterLiveState?
    ): Pair<StatRegistry, List<SkillModel>> {
        val transformationId = liveState?.transformationId?.takeIf { it.isNotBlank() } ?: return statRegistry to skills
        val monster = runCatching { monstersDataSource.getMonster(transformationId) }.getOrNull()
        if (monster == null) {
            Log.w(TAG, "Transformation monster $transformationId not found, skipping stats override.")
            return statRegistry to skills
        }

        val config = TransformationApplier.Configuration(
            stats = monster.stats.mapKeys { (key, _) -> key.uppercase() },
            skillBonuses = emptyMap()
        )
        val result = transformationApplier.apply(statRegistry, skills, config)
        return result.statRegistry to result.skills
    }

    private suspend fun fetchClassMetadata(draft: DraftCharacter): Map<String, ClassEntity> {
        val indices = draft.levelStack.map { it.classIndex }.distinct()
        return indices.mapNotNull { idx -> classFeatureRepository.getClassEntity(idx)?.let { idx to it } }.toMap()
    }

    private suspend fun fetchProgressionHistory(draft: DraftCharacter): List<ProgressionEntity> = coroutineScope {
        val classLevelCounters = mutableMapOf<String, Int>()

        val allRows = draft.levelStack.map { step ->
            val classIdx = step.classIndex
            val currentClassLevel = classLevelCounters.getOrDefault(classIdx, 0) + 1
            classLevelCounters[classIdx] = currentClassLevel

            async {
                val levelRows = classFeatureRepository.getProgressionForLevel(classIdx, currentClassLevel)
                levelRows.filter { row ->
                    row.subclassIndex == null || row.subclassIndex == step.subclassIndex
                }
            }
        }.awaitAll().flatten()

        allRows
    }

    private fun resolveSelectedFamiliarId(draft: DraftCharacter): String? {
        val candidates = setOf("imp", "pseudodragon", "quasit", "sprite")
        fun findInSelections(selections: Map<String, ChoiceResult>): String? {
            selections.forEach { (key, value) ->
                if (!key.contains("pact-of-the-chain", ignoreCase = true)) return@forEach
                val selected = value as? ChoiceResult.SelectedOptions ?: return@forEach
                selected.items.firstOrNull { it in candidates }?.let { return it }
            }
            return null
        }

        findInSelections(draft.baseInfo.raceSelections)?.let { return it }
        findInSelections(draft.baseInfo.backgroundSelections)?.let { return it }
        draft.levelStack.forEach { step ->
            findInSelections(step.selections)?.let { return it }
        }

        val allSelections = draft.baseInfo.raceSelections.values +
            draft.baseInfo.backgroundSelections.values +
            draft.levelStack.flatMap { it.selections.values }
        return allSelections.filterIsInstance<ChoiceResult.SelectedOptions>()
            .flatMap { it.items }
            .firstOrNull { it in candidates }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\SnapshotAssembler.kt
