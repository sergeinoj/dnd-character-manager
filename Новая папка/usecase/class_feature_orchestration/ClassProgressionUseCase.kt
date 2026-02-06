// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/ClassProgressionUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.model.ProgressionSpellcastingJson
import com.dnd.app.data.repository.datasource.SpellDataSource
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassProgressionUseCase @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository, // ИЗМЕНЕНО
    private val spellDataSource: SpellDataSource,
    private val featureFactory: FeatureFactory
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_PROG_UC"

    suspend fun generateInitialFeatures(classEntity: ClassEntity, abilityModifier: Int): List<Feature> {
        val features = mutableListOf<Feature>()
        features.addAll(createInitialProficiencyFeatures(classEntity))
        features.addAll(createInitialSpellFeatures(classEntity, abilityModifier))
        // [НОВЫЙ ВЫЗОВ v1.25]
        features.addAll(createInitialEquipmentFeatures(classEntity))
        return features
    }

    suspend fun generateLevelUpFeatures(classIndex: String, newLevel: Int): List<Feature> {
        if (newLevel <= 1) return emptyList()
        return createSpellDeltaFeatures(classIndex, newLevel)
    }

    private suspend fun createInitialProficiencyFeatures(classEntity: ClassEntity): List<Feature> {
        val rawJson = classEntity.proficiencyChoicesJson ?: return emptyList()
        return try {
            json.decodeFromString<List<JsonObject>>(rawJson).mapIndexedNotNull { i, choiceJson ->
                val choiceDomain = featureFactory.parseChoice(choiceJson)

                val type = choiceJson["type"]?.jsonPrimitive?.content ?: "proficiency_$i"
                val name = DndLocalization.translateFeatureChoiceHeader(type).ifBlank { "Владения класса" }

                Feature(
                    id = -1 - i,
                    index = "virtual-prof-choice-${classEntity.indexName}-${i}",
                    name = name,
                    description = choiceJson["desc"]?.jsonPrimitive?.content ?: "Выберите владения для вашего персонажа.",
                    choices = listOf(choiceDomain),
                    uiGroup = "SKILLS"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create virtual proficiency features for ${classEntity.indexName}", e)
            emptyList()
        }
    }

    /**
     * [НОВЫЙ МЕТОД v1.25]
     * Создает виртуальные способности для выбора стартового снаряжения.
     */
    private suspend fun createInitialEquipmentFeatures(classEntity: ClassEntity): List<Feature> {
        val rawJson = classEntity.startingEquipmentOptionsJson ?: return emptyList()
        return try {
            json.decodeFromString<List<JsonObject>>(rawJson).mapIndexedNotNull { i, choiceJson ->
                val choiceDomain = featureFactory.parseChoice(choiceJson)
                val desc = choiceJson["desc"]?.jsonPrimitive?.content ?: "Выберите снаряжение"
                Feature(
                    id = -200 - i, // Уникальный диапазон ID для снаряжения
                    index = "virtual-equip-choice-${classEntity.indexName}-${i}",
                    name = desc,
                    description = "", // Описание в названии
                    choices = listOf(choiceDomain),
                    uiGroup = "INVENTORY"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create virtual equipment features for ${classEntity.indexName}", e)
            emptyList()
        }
    }

    private suspend fun createInitialSpellFeatures(classEntity: ClassEntity, abilityModifier: Int): List<Feature> {
        val rawJson = classEntity.spellcastingJson ?: return emptyList()
        val features = mutableListOf<Feature>()
        try {
            val spellcastingInfo = json.parseToJsonElement(rawJson).jsonObject
            val progressionRow = classFeatureRepository.getProgressionForLevel(classEntity.indexName, 1).firstOrNull()

            val cantripsKnown = spellcastingInfo["cantrips_known"]?.jsonPrimitive?.int
                ?: progressionRow?.spellcastingJson?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it).cantripsKnown }.getOrNull() } ?: 0

            val spellsKnown = spellcastingInfo["spells_known"]?.jsonPrimitive?.int
                ?: progressionRow?.spellcastingJson?.let { runCatching { json.decodeFromString<ProgressionSpellcastingJson>(it).spellsKnown }.getOrNull() } ?: 0

            if (cantripsKnown > 0) {
                val allSpells = spellDataSource.getSpellsByLevelAndClass(0, classEntity.indexName)
                val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                features.add(Feature(id = -50, index = "virtual-initial-cantrips", name = "Заговоры",
                    description = "Выберите заговоры.",
                    choices = listOf(FeatureChoiceDomain.SelectSpell(cantripsKnown, "class_cantrips", options)), uiGroup = "SPELLS"))
            }
            if (spellsKnown > 0) {
                val allSpells = spellDataSource.getSpellsByLevelAndClass(1, classEntity.indexName)
                val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                features.add(Feature(id = -51, index = "virtual-initial-spells", name = "Заклинания 1-го уровня",
                    description = "Выберите заклинания.",
                    choices = listOf(FeatureChoiceDomain.SelectSpell(spellsKnown, "class_spells", options)), uiGroup = "SPELLS"))
            }

            // [НОВЫЙ БЛОК v1.27] - Логика подготовленных заклинаний
            if (classEntity.indexName == "cleric") {
                val level = 1 // Этот метод вызывается только для 1 уровня
                val preparedCount = (abilityModifier + level).coerceAtLeast(1)
                if (preparedCount > 0) {
                    val allSpells = spellDataSource.getSpellsByLevelAndClass(1, "cleric")
                    val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
                    features.add(
                        Feature(
                            id = -52, // Новый уникальный ID
                            index = "virtual-prepared-spells",
                            name = "Подготовка заклинаний",
                            description = "Вы можете подготовить количество заклинаний 1-го уровня, равное вашему модификатору Мудрости + ваш уровень Жреца (минимум одно заклинание).",
                            choices = listOf(
                                FeatureChoiceDomain.SelectSpell(
                                    count = preparedCount,
                                    poolType = "class_prepared_spells",
                                    options = options
                                )
                            ),
                            uiGroup = "SPELLS"
                        )
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse initial spell features for ${classEntity.indexName}", e)
        }
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
            deltaFeatures.add(Feature(id = -2 - level, index = "virtual-cantrip-choice-level-$level", name = "Новый заговор",
                description = "Вы изучаете новый заговор.",
                choices = listOf(FeatureChoiceDomain.SelectSpell(deltaCantrips, "class_cantrips", options)), uiGroup = "SPELLS"))
        }

        val deltaSpellsKnown = (currentSpells?.spellsKnown ?: 0) - (prevSpells?.spellsKnown ?: 0)
        if (deltaSpellsKnown > 0) {
            val allSpells = spellDataSource.getAllSpellsByClass(classIndex).filter { it.level > 0 }
            val options = allSpells.map { ChoiceOption(it.index, it.name, spell = it) }
            deltaFeatures.add(Feature(id = -100 - level, index = "virtual-spell-choice-level-$level", name = "Новое заклинание",
                description = "Вы изучаете новое заклинание.",
                choices = listOf(FeatureChoiceDomain.SelectSpell(deltaSpellsKnown, "class_spells", options)), uiGroup = "SPELLS"))
        }
        return deltaFeatures
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/ClassProgressionUseCase.kt