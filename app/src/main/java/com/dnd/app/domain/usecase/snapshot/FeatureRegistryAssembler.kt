package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.FeatureDisplayModel
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRegistryAssembler @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val json: Json
) {
    suspend fun assemble(
        draft: DraftCharacter,
        statRegistry: StatRegistry,
        classMetadata: Map<String, ClassEntity>,
        allProficiencies: Map<String, Int>,
        raceEntity: Race?,
        background: Background?
    ): FeatureRegistryResult = coroutineScope {

        val levelFeatures = mutableListOf<Feature>()
        levelFeatures.addAll(raceEntity?.let { libraryRepository.getBaseRaceFeatures(it.id) } ?: emptyList())
        levelFeatures.addAll(draft.baseInfo.subraceIndex?.let { libraryRepository.getSubraceFeatures(it) } ?: emptyList())
        levelFeatures.addAll(background?.features ?: emptyList())

        val classLevelsEncountered = mutableMapOf<String, Int>()
        draft.levelStack.mapIndexed { index, step ->
            val lvl = (classLevelsEncountered[step.classIndex] ?: 0) + 1
            classLevelsEncountered[step.classIndex] = lvl
            val statMod = statRegistry.modifiers[classMetadata[step.classIndex]?.primaryStat ?: "CHA"] ?: 0

            async {
                libraryRepository.getClassFeaturesForLevel(
                    classIndex = step.classIndex,
                    level = lvl,
                    subclassIndex = step.subclassIndex,
                    abilityModifier = statMod,
                    isGenesis = index == 0,
                    proficiencyProvider = { allProficiencies }
                )
            }
        }.awaitAll().forEach { res ->
            levelFeatures.addAll(res.baseClassFeatures)
            levelFeatures.addAll(res.selectedSubclassFeatures)
        }

        val resolved = mutableMapOf<String, Feature>()
        levelFeatures.forEach { resolved[it.index] = it }

        val toResolve = mutableSetOf<String>()
        fun scan(map: Map<String, ChoiceResult>) {
            map.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { res ->
                res.items.forEach { if (it.length > 2) toResolve.add(it) }
            }
        }
        scan(draft.baseInfo.raceSelections); scan(draft.baseInfo.backgroundSelections); draft.levelStack.forEach { scan(it.selections) }

        var iteration = 0
        while (iteration < 10) {
            val pending = toResolve.filter { it !in resolved }
            if (pending.isEmpty()) break

            val newly = pending.mapNotNull { libraryRepository.getFeatureByIndex(it) }
            if (newly.isEmpty()) break

            for (f in newly) {
                resolved[f.index] = f
                f.grantedProficiencies.forEach { if (it.length > 2) toResolve.add(it) }
            }
            iteration++
        }

        val all = resolved.values.toList()
        var hpB = 0; val totalL = draft.levelStack.size.coerceAtLeast(1)

        for (f in all) {
            var appliedFromJson = false
            f.referenceJson?.let { raw ->
                runCatching {
                    val obj = json.parseToJsonElement(raw).jsonObject
                    if (obj["mechanic_type"]?.jsonPrimitive?.content == "HP_PER_LEVEL") {
                        hpB += totalL
                        appliedFromJson = true
                    } else {
                        val perLevel = obj["hp_bonus_per_level"]?.jsonPrimitive?.intOrNull
                        val flat = obj["hp_bonus_flat"]?.jsonPrimitive?.intOrNull
                        if (perLevel != null || flat != null) {
                            hpB += (perLevel ?: 0) * totalL
                            hpB += (flat ?: 0)
                            appliedFromJson = true
                        }
                    }
                }
            }
            if (!appliedFromJson && f.index == "draconic-resilience") {
                hpB += totalL
            }
        }

        val models = all
            .filter { !it.index.startsWith("virtual-") && it.index != "desc" && it.name.isNotBlank() }
            .map { f ->
                val resolvedSource = when {
                    f.index.startsWith("bgf-") || f.classIndex == "background" -> "Предыстория"
                    !f.raceIndex.isNullOrBlank() || !f.subraceIndex.isNullOrBlank() -> "Раса"
                    !f.subclassIndex.isNullOrBlank() -> "Подкласс"
                    !f.classIndex.isNullOrBlank() -> "Класс"
                    else -> f.uiGroup
                } ?: "Класс"

                FeatureDisplayModel(
                    id = f.index,
                    name = f.name.stripHtml(),
                    description = f.description.stripHtml(),
                    source = resolvedSource,
                    hasChoices = f.choices.isNotEmpty(),
                    level = f.level,
                    displayPriority = f.priority,
                    poolId = if (f.maxCharges > 0) f.index else null,
                    resetRule = f.resetRule,
                    referenceJson = f.referenceJson
                )
            }
            .sortedBy { it.name }

        FeatureRegistryResult(all, models, hpB)
    }
}
