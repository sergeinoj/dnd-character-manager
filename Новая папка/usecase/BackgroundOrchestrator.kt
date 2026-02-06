// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/BackgroundOrchestrator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.BackgroundEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.model.ChoiceJson
import com.dnd.app.data.model.OptionJson
import com.dnd.app.data.model.OptionSetJson
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ОБНОВЛЕНО v1.28]
 * Оркестратор, отвечающий за полное преобразование BackgroundEntity в доменную модель Background.
 * Работает в режиме ОБОГАЩЕНИЯ: берет фичи (`bgf-*`) из базы, наполняет их читаемым описанием
 * и выборами. Использует поля из `backgrounds` как fallback, если фичи не предоставляют
 * всей необходимой информации (например, выбор языков).
 */
@Singleton
class BackgroundOrchestrator @Inject constructor(
    private val referenceDao: ReferenceDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val TAG = "DND_DEBUG_BG_ORCH"

    // Внутренний класс для отслеживания состояния парсинга
    private data class ProcessingState(
        var languageChoiceProvided: Boolean = false,
        var equipmentChoiceProvided: Boolean = false,
        val allFeatures: MutableList<Feature> = mutableListOf()
    )

    suspend fun execute(
        entity: BackgroundEntity,
        allFeaturesMap: Map<String, FeatureEntity>
    ): Background {
        val state = ProcessingState()

        // --- Шаг 1: Обогащение связанных фич (`bgf-*`) ---
        val featureIndices = entity.featureIndicesJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
        } ?: emptyList()

        featureIndices.forEach { index ->
            allFeaturesMap[index]?.let { featEntity ->
                val feature = enrichBackgroundFeature(featEntity, state)
                state.allFeatures.add(feature)
            }
        }

        // --- Шаг 2: Умная дедупликация (Smart Fallback) ---
        // Языки: добавляем выбор только если его не было в фичах
        if (!state.languageChoiceProvided) {
            createLanguageChoiceFeature(entity)?.let { state.allFeatures.add(it) }
        }

        // Снаряжение: добавляем выбор только если его не было в фичах
        if (!state.equipmentChoiceProvided) {
            entity.startingEquipmentOptionsJson?.let { raw ->
                try {
                    val choices = json.decodeFromString<List<ChoiceJson>>(raw)
                    choices.forEachIndexed { i, choiceJson ->
                        val choiceDomain = parseChoice(choiceJson, state)
                        state.allFeatures.add(
                            Feature(
                                id = -500 - (entity.id ?: 0) - i,
                                index = "bg-equip-choice-${entity.indexName}-$i",
                                name = (choiceDomain as? FeatureChoiceDomain.SelectOption)?.description ?: "Снаряжение",
                                description = "Выберите стартовое снаряжение.",
                                choices = listOf(choiceDomain),
                                uiGroup = "INVENTORY",
                                priority = 200 // После владений
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse fallback starting_equipment_options_json for ${entity.indexName}", e)
                }
            }
        }

        // --- Шаг 3: Добавление базовой фичи, если она не была включена в список ---
        // (Для старых предысторий без `feature_indices_json`)
        if (featureIndices.isEmpty() && entity.featureIndex != null) {
            val baseFeature = Feature(
                id = -400 - (entity.id ?: 0),
                index = entity.featureIndex,
                name = entity.featureName ?: "Умение предыстории",
                description = entity.featureDesc?.stripHtml() ?: "",
                priority = 1
            )
            state.allFeatures.add(baseFeature)
        }

        // --- Финальная сборка модели Background ---
        val staticEquipment = parseSimpleReference(entity.startingEquipmentJson)
        val personalityTraits = parseJsonStrings(entity.personalityTraitsJson)
        val ideals = parseJsonStrings(entity.idealsJson)
        val bonds = parseJsonStrings(entity.bondsJson)
        val flaws = parseJsonStrings(entity.flawsJson)

        return Background(
            id = entity.id ?: 0,
            name = entity.name,
            features = state.allFeatures.distinctBy { it.index }.sortedBy { it.priority },
            equipment = staticEquipment,
            startingGold = entity.startingGold ?: 0,
            featureIndices = featureIndices,
            personalityTraits = personalityTraits,
            ideals = ideals,
            bonds = bonds,
            flaws = flaws
        )
    }

    private suspend fun enrichBackgroundFeature(entity: FeatureEntity, state: ProcessingState): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        entity.choicesJson?.let { raw ->
            try {
                val el = json.parseToJsonElement(raw)
                val elements = if (el is JsonArray) el else listOf(el)
                elements.forEach {
                    if (it is JsonObject) choices.add(parseChoice(json.decodeFromJsonElement(it), state))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing choices_json for bg feature ${entity.indexName}", e)
            }
        }

        // Извлекаем гарантированные владения из фичи
        val grantedProficiencies = parseSimpleReference(entity.referenceJson, "granted_proficiencies")

        // Обогащаем описание, если это фича на владения
        var enrichedDescription = entity.description?.stripHtml() ?: ""
        if (entity.indexName.contains("-skills") && grantedProficiencies.isNotEmpty()) {
            val allProfEntities = referenceDao.getAllProficiencies().associateBy { it.indexName }
            val allSkillEntities = referenceDao.getAllSkills().associateBy { it.indexName }

            enrichedDescription = grantedProficiencies.sorted().joinToString("\n") { index ->
                val cleanIndex = index.replace("skill-", "")
                val name = allProfEntities[index]?.name
                    ?: allSkillEntities[cleanIndex]?.name
                    ?: DndLocalization.translateSkill(index)
                "• $name"
            }
        }

        val priority = when {
            entity.indexName.contains("-lore") -> 1
            entity.indexName.contains("-skills") -> 10
            choices.isNotEmpty() -> 20
            else -> 100
        }

        return Feature(
            id = entity.id ?: 0,
            index = entity.indexName,
            name = entity.name,
            description = enrichedDescription,
            choices = choices,
            grantedProficiencies = grantedProficiencies,
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup ?: "BACKGROUND",
            priority = priority
        )
    }

    private suspend fun createLanguageChoiceFeature(entity: BackgroundEntity): Feature? {
        val rawJson = entity.languageOptionsJson ?: return null
        return try {
            val choiceJson = json.decodeFromString<ChoiceJson>(rawJson)
            val allLanguages = referenceDao.getAllLanguages()
            val options = allLanguages.map { ChoiceOption(it.indexName, it.name) }

            Feature(
                id = -450 - (entity.id ?: 0),
                index = "bg-lang-choice-${entity.indexName}",
                name = "Дополнительные языки",
                description = "Выберите дополнительный язык, который знает ваш персонаж.",
                choices = listOf(
                    FeatureChoiceDomain.SelectOption(
                        count = choiceJson.choose,
                        options = options,
                        description = choiceJson.desc
                    )
                ),
                uiGroup = "BACKGROUND",
                priority = 50 // После основной информации
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse language_options_json for ${entity.indexName}", e)
            null
        }
    }

    private suspend fun parseChoice(choiceJson: ChoiceJson, state: ProcessingState): FeatureChoiceDomain {
        val options = choiceJson.from?.let { parseOptions(it, state) } ?: emptyList()

        // Классификация типа выбора для дедупликации
        if (choiceJson.from?.resource == "languages") {
            state.languageChoiceProvided = true
        } else if (choiceJson.from?.equipmentCategory != null || choiceJson.from?.resource?.contains("tools") == true) {
            state.equipmentChoiceProvided = true
        }

        return FeatureChoiceDomain.SelectOption(
            count = choiceJson.choose,
            options = options,
            description = choiceJson.desc
        )
    }

    private suspend fun parseOptions(optionSet: OptionSetJson, state: ProcessingState): List<ChoiceOption> {
        // Явные опции из массива `options`
        val explicitOptions = optionSet.options?.mapNotNull { parseOption(it) } ?: emptyList()
        if (explicitOptions.isNotEmpty()) return explicitOptions

        // Динамические опции из `resource_list`
        if (optionSet.optionSetType == "resource_list") {
            return when (val resource = optionSet.resource) {
                "languages" -> {
                    state.languageChoiceProvided = true
                    referenceDao.getAllLanguages().map { ChoiceOption(id = it.indexName, label = it.name) }
                }
                "artisan-tools" -> {
                    state.equipmentChoiceProvided = true
                    val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive("artisans-tools")
                    val equipment = referenceDao.getEquipmentByIndexes(itemIndexes)
                    equipment.map { ChoiceOption(id = it.indexName, label = it.name, info = it.description) }
                }
                else -> {
                    Log.w(TAG, "Unknown resource list type in background feature: $resource")
                    emptyList()
                }
            }
        }

        // Динамические опции из `equipment_category`
        if (optionSet.optionSetType == "equipment_category") {
            val categoryIndex = optionSet.equipmentCategory?.index
            if (categoryIndex != null) {
                state.equipmentChoiceProvided = true
                val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive(categoryIndex)
                if (itemIndexes.isNotEmpty()) {
                    val equipment = referenceDao.getEquipmentByIndexes(itemIndexes)
                    return equipment.map {
                        ChoiceOption(
                            id = it.indexName,
                            label = it.name,
                            info = it.description
                        )
                    }
                }
            }
        }
        return emptyList()
    }

    private fun parseOption(optionJson: OptionJson): ChoiceOption? {
        val id = optionJson.item?.index ?: optionJson.string ?: return null
        val label = optionJson.item?.name ?: DndLocalization.translateSkill(id)
        return ChoiceOption(id, label, optionJson.desc)
    }

    private fun parseSimpleReference(raw: String?, key: String? = null): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val element = json.parseToJsonElement(raw)
            val targetArray = if (key != null && element is JsonObject) {
                element[key]?.jsonArray
            } else {
                element as? JsonArray
            }

            targetArray?.mapNotNull {
                it.jsonObject["index"]?.jsonPrimitive?.content
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse simple reference JSON: $raw", e)
            emptyList()
        }
    }

    private fun parseJsonStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse string array JSON: $raw", e)
            emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/BackgroundOrchestrator.kt