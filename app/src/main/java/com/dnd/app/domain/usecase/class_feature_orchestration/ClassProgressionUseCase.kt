// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\ClassProgressionUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.model.ClassSpecificJson
import com.dnd.app.data.model.MultiClassingJson
import com.dnd.app.data.model.ProgressionSpellcastingJson
import com.dnd.app.data.repository.datasource.SpellDataSource
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import com.dnd.app.domain.model.MulticlassRequirement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassProgressionUseCase @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val spellDataSource: SpellDataSource,
    private val featureFactory: FeatureFactory,
    private val calculator: DndCalculator
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_PROG_UC"


    suspend fun generateInitialFeatures(
        classEntity: ClassEntity,
        abilityModifier: Int,
        isGenesis: Boolean
    ): List<Feature> {
        val features = mutableListOf<Feature>()


        if (isGenesis) {

            features.addAll(createInitialProficiencyFeatures(classEntity))
            features.addAll(createInitialEquipmentFeatures(classEntity))
        } else {

            features.addAll(parseMulticlassProficiencies(classEntity))
        }

        features.addAll(createInitialSpellFeatures(classEntity, abilityModifier))
        return features
    }

    suspend fun generateLevelUpFeatures(classIndex: String, newLevel: Int): List<Feature> {
        if (newLevel <= 1) return emptyList()
        return createSpellDeltaFeatures(classIndex, newLevel)
    }


    private suspend fun parseMulticlassProficiencies(classEntity: ClassEntity): List<Feature> {
        val rawMc = classEntity.multiClassingJson ?: return emptyList()
        return try {
            val mcData = json.decodeFromString<MultiClassingJson>(rawMc)


            val staticIndexes = mcData.proficiencies.map { it.index }


            val dynamicChoices = mcData.proficiencyChoices.mapIndexed { i, choiceJson ->
                featureFactory.parseChoice(
                    obj = choiceJson,
                    parentPath = "virtual-mc-choice-${classEntity.indexName}-$i"
                )
            }

            if (staticIndexes.isEmpty() && dynamicChoices.isEmpty()) return emptyList()


            val staticNames = mcData.proficiencies.map { it.name }.joinToString(", ")
            val desc = if (staticNames.isNotBlank()) "Вы получаете владение: $staticNames." else ""

            listOf(
            Feature(
                id = -1001,
                index = "virtual-mc-prof-${classEntity.indexName}",
                name = "Владения мультикласса",
                description = desc,
                choices = dynamicChoices,
                grantedProficiencies = staticIndexes,
                uiGroup = "SKILLS"
            )
        )
    } catch (e: Exception) {
            Log.e(TAG, "Failed to parse multiclass profs for ${classEntity.indexName}", e)
        emptyList()
    }
}

    suspend fun parseMulticlassPrerequisites(classEntity: ClassEntity): List<MulticlassRequirement> {
        val rawMc = classEntity.multiClassingJson ?: return emptyList()
        return try {
            val mcData = json.decodeFromString<MultiClassingJson>(rawMc)
            mcData.prerequisites.mapNotNull { prereq ->
                val statCode = prereq.abilityScore?.index
                    ?.uppercase()
                    ?.take(3)
                    ?.takeIf { it.isNotBlank() }
                    ?: prereq.abilityScore?.name
                        ?.uppercase()
                        ?.take(3)
                        ?.takeIf { it.isNotBlank() }
                val minScore = prereq.minimumScore
                if (statCode == null || minScore <= 0) return@mapNotNull null
                MulticlassRequirement(statCode, minScore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse multiclass prerequisites for ${classEntity.indexName}", e)
            emptyList()
        }
    }

    private suspend fun createInitialProficiencyFeatures(classEntity: ClassEntity): List<Feature> {
        val existingSkillFeatures = classFeatureRepository.findFeaturesByContext(classIndex = classEntity.indexName, level = 1)
        if (existingSkillFeatures.any { it.indexName.contains("-skills") }) return emptyList()

        val rawJson = classEntity.proficiencyChoicesJson ?: return emptyList()
        return try {
            json.decodeFromString<List<JsonObject>>(rawJson).mapIndexedNotNull { i, choiceJson ->
                val parentPath = DndConstants.VirtualKeys.initialProficiencyChoice(classEntity.indexName, i)
                val choiceDomain = featureFactory.parseChoice(choiceJson, parentPath = parentPath)
                val type = choiceJson["type"]?.jsonPrimitive?.content ?: "proficiency_$i"
                val name = DndLocalization.translateFeatureChoiceHeader(type).ifBlank { "Владения класса" }

                Feature(
                    id = -1 - i,
                    index = parentPath,
                    name = name,
                    description = choiceJson["desc"]?.jsonPrimitive?.content ?: "Выберите владения.",
                    choices = listOf(choiceDomain),
                    uiGroup = "SKILLS"
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun createInitialEquipmentFeatures(classEntity: ClassEntity): List<Feature> {
        val rawJson = classEntity.startingEquipmentOptionsJson ?: return emptyList()
        return try {
            json.decodeFromString<List<JsonObject>>(rawJson).mapIndexedNotNull { i, choiceJson ->
                val desc = choiceJson["desc"]?.jsonPrimitive?.content ?: "Выберите снаряжение"
                val featureIndex = choiceJson["index"]?.jsonPrimitive?.content
                val finalIndex = featureIndex.takeIf { !it.isNullOrBlank() }
                    ?: DndConstants.VirtualKeys.initialEquipmentChoice(classEntity.indexName, i)

                val choiceDomain = featureFactory.parseChoice(choiceJson, parentPath = finalIndex)
                Feature(id = -200 - i, index = finalIndex, name = desc, description = "", choices = listOf(choiceDomain), uiGroup = "INVENTORY")
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun createInitialSpellFeatures(classEntity: ClassEntity, abilityModifier: Int): List<Feature> {
        val features = mutableListOf<Feature>()
        try {
            val progressionRow = classFeatureRepository.getProgressionForLevel(classEntity.indexName, 1).firstOrNull()
            val spellcastingInfo = classEntity.spellcastingJson
                ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }

            val cantripsKnown = spellcastingInfo?.get("cantrips_known")?.jsonPrimitive?.int
                ?: progressionRow?.spellcastingJson?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it).cantripsKnown }.getOrNull() } ?: 0

            val spellsKnown = spellcastingInfo?.get("spells_known")?.jsonPrimitive?.int
                ?: progressionRow?.spellcastingJson?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it).spellsKnown }.getOrNull() } ?: 0

            if (cantripsKnown > 0) {
                val allSpells = spellDataSource.getSpellsByLevelAndClass(0, classEntity.indexName)
                val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                features.add(Feature(id = -50, index = DndConstants.VirtualKeys.INITIAL_CANTRIPS, name = "Заговоры",
                    description = "Выберите заговоры.",
                    choices = listOf(FeatureChoiceDomain.SelectSpell(cantripsKnown, "class_cantrips", options)), uiGroup = "SPELLS"))
            }
            if (spellsKnown > 0) {
                val allSpells = spellDataSource.getSpellsByLevelAndClass(1, classEntity.indexName)
                val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                features.add(Feature(id = -51, index = DndConstants.VirtualKeys.INITIAL_SPELLS, name = "Заклинания 1-го уровня",
                    description = "Выберите заклинания.",
                    choices = listOf(FeatureChoiceDomain.SelectSpell(spellsKnown, "class_spells", options)), uiGroup = "SPELLS"))
            }

            val classSpecific = progressionRow?.classSpecificJson?.let { runCatching { json.decodeFromString<ClassSpecificJson>(it) }.getOrNull() }
            val prepRule = classSpecific?.preparationRule
            val dynamicPreparation = progressionRow?.spellcastingJson
                ?.let { raw ->
                    runCatching {
                        json.parseToJsonElement(raw).jsonObject["dynamic_preparation"]?.jsonPrimitive?.booleanOrNull == true
                    }.getOrDefault(false)
                } == true
            val supportsPreparedByData = prepRule != null || (dynamicPreparation && spellsKnown == 0)
            val preparedCount = when {
                prepRule != null -> calculator.resolvePreparationFormula(prepRule.formula, 1, abilityModifier, prepRule.minLimit)
                supportsPreparedByData && progressionRow?.prepFormulaType.equals("FULL", ignoreCase = true) -> calculator.resolvePreparationFormula("LEVEL_PLUS_MOD", 1, abilityModifier, 1)
                supportsPreparedByData && progressionRow?.prepFormulaType.equals("HALF", ignoreCase = true) -> calculator.resolvePreparationFormula("HALF_LEVEL_PLUS_MOD", 1, abilityModifier, 1)
                else -> 0
            }

            if (preparedCount > 0) {
                    val allSpells = spellDataSource.getSpellsByLevelAndClass(1, classEntity.indexName)
                    val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                    val featureIndex = DndConstants.VirtualKeys.preparedSpellsForClass(classEntity.indexName)
                    features.add(Feature(id = -52, index = featureIndex, name = "Подготовка заклинаний",
                        description = "Вы можете подготовить заклинания.",
                        choices = listOf(FeatureChoiceDomain.SelectSpell(preparedCount, "class_prepared_spells", options, autoAdjustLimit = true)),
                        uiGroup = "SPELLS"))
            }
        } catch (e: Exception) { }
        return features
    }

    private suspend fun createSpellDeltaFeatures(classIndex: String, level: Int): List<Feature> {
        val progressionData = classFeatureRepository.getProgressionForLevels(classIndex, listOf(level, level - 1))
        val currentLevelData = progressionData.find { it.level == level }?.spellcastingJson
        val prevLevelData = progressionData.find { it.level == level - 1 }?.spellcastingJson

        val currentSpells = currentLevelData?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it) }.getOrNull() }
        val prevSpells = prevLevelData?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it) }.getOrNull() }

        val deltaFeatures = mutableListOf<Feature>()
        val deltaCantrips = (currentSpells?.cantripsKnown ?: 0) - (prevSpells?.cantripsKnown ?: 0)
        if (deltaCantrips > 0) {
            val allSpells = spellDataSource.getSpellsByLevelAndClass(0, classIndex)
            val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
            deltaFeatures.add(Feature(id = -2 - level, index = DndConstants.VirtualKeys.levelUpCantripChoice(level), name = "Новый заговор",
                description = "Вы изучаете новый заговор.",
                choices = listOf(FeatureChoiceDomain.SelectSpell(deltaCantrips, "class_cantrips", options)), uiGroup = "SPELLS"))
        }

        val deltaSpellsKnown = (currentSpells?.spellsKnown ?: 0) - (prevSpells?.spellsKnown ?: 0)
        if (deltaSpellsKnown > 0) {
            val allSpells = spellDataSource.getAllSpellsByClass(classIndex).filter { it.level > 0 }
            val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
            deltaFeatures.add(Feature(id = -100 - level, index = DndConstants.VirtualKeys.levelUpSpellChoice(level), name = "Новое заклинание",
                description = "Вы изучаете новое заклинание.",
                choices = listOf(FeatureChoiceDomain.SelectSpell(deltaSpellsKnown, "class_spells", options)), uiGroup = "SPELLS"))
        }
        return deltaFeatures
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\ClassProgressionUseCase.kt
