// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/BackgroundOrchestrator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.BackgroundEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ОБНОВЛЕНО v1.32]
 * Оркестратор, отвечающий за полное преобразование BackgroundEntity в доменную модель Background.
 * Теперь работает ИСКЛЮЧИТЕЛЬНО на основе связанных фич (`bgf-*`) из поля `feature_indices_json`.
 * Все устаревшие механизмы (fallback) для создания "фантомных" карточек выбора удалены.
 * Реализован гибкий парсер JSON, который корректно обрабатывает структуру `choices_json` для предысторий.
 */
@Singleton
class BackgroundOrchestrator @Inject constructor(
    private val referenceDao: ReferenceDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val TAG = "DND_DEBUG_BG_ORCH"

    suspend fun execute(
        entity: BackgroundEntity,
        allFeaturesMap: Map<String, FeatureEntity>
    ): Background {
        // --- Шаг 1: Обогащение связанных фич (`bgf-*`) ---
        val featureIndices = entity.featureIndicesJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() }
        } ?: emptyList()

        val allFeatures = featureIndices.mapNotNull { index ->
            allFeaturesMap[index]?.let { featEntity ->
                enrichBackgroundFeature(featEntity)
            }
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
            features = allFeatures.distinctBy { it.index }.sortedBy { it.priority },
            equipment = staticEquipment,
            startingGold = entity.startingGold ?: 0,
            featureIndices = featureIndices,
            personalityTraits = personalityTraits,
            ideals = ideals,
            bonds = bonds,
            flaws = flaws
        )
    }

    private suspend fun enrichBackgroundFeature(entity: FeatureEntity): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        entity.choicesJson?.let { raw ->
            try {
                val el = json.parseToJsonElement(raw)
                val elements = if (el is JsonArray) el else listOf(el)
                elements.forEach {
                    if (it is JsonObject) choices.add(parseChoice(it))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing choices_json for bg feature ${entity.indexName}", e)
            }
        }

        val grantedProficiencies = parseSimpleReference(entity.referenceJson, "granted_proficiencies")

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

    private suspend fun parseChoice(choiceJsonObj: JsonObject): FeatureChoiceDomain {
        // Используем ручной парсинг вместо decodeFromJsonElement для гибкости
        val count = choiceJsonObj["choose"]?.jsonPrimitive?.int ?: 1
        val desc = choiceJsonObj["desc"]?.jsonPrimitive?.content
        // [КЛЮЧЕВОЕ ИЗМЕНЕНИЕ] Извлекаем тип ресурса из родительского объекта
        val parentType = choiceJsonObj["type"]?.jsonPrimitive?.content
        val fromElement = choiceJsonObj["from"]

        val options = parseOptions(fromElement, parentType)

        return FeatureChoiceDomain.SelectOption(
            count = count,
            options = options,
            description = desc
        )
    }

    private suspend fun parseOptions(optionSetElement: JsonElement?, parentType: String?): List<ChoiceOption> {
        if (optionSetElement !is JsonObject) return emptyList()

        val optionSet = optionSetElement.jsonObject
        val optionSetType = optionSet["option_set_type"]?.jsonPrimitive?.content

        // [КЛЮЧЕВОЕ ИЗМЕНЕНИЕ] Гибкое определение типа ресурса
        if (optionSetType == "resource_list") {
            val resourceFromInside = optionSet["resource"]?.jsonPrimitive?.content
            val effectiveResource = resourceFromInside ?: parentType // Fallback на тип из родителя

            return when {
                effectiveResource?.contains("languages") == true ->
                    referenceDao.getAllLanguages().map { ChoiceOption(id = it.indexName, label = it.name) }
                effectiveResource?.contains("artisan-tools") == true -> {
                    val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive("artisans-tools")
                    referenceDao.getEquipmentByIndexes(itemIndexes).map { ChoiceOption(id = it.indexName, label = it.name, info = it.description) }
                }
                effectiveResource?.contains("gaming-sets") == true -> {
                    val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive("gaming-sets")
                    referenceDao.getEquipmentByIndexes(itemIndexes).map { ChoiceOption(id = it.indexName, label = it.name, info = it.description) }
                }
                effectiveResource?.contains("instruments") == true -> {
                    val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive("musical-instruments")
                    referenceDao.getEquipmentByIndexes(itemIndexes).map { ChoiceOption(id = it.indexName, label = it.name, info = it.description) }
                }
                else -> {
                    Log.w(TAG, "Unknown resource list type in background feature: $effectiveResource")
                    emptyList()
                }
            }
        }

        if (optionSetType == "equipment_category") {
            val categoryIndex = optionSet["equipment_category"]?.jsonObject?.get("index")?.jsonPrimitive?.content
            if (categoryIndex != null) {
                val itemIndexes = referenceDao.getAllItemIndexesByCategoryRecursive(categoryIndex)
                if (itemIndexes.isNotEmpty()) {
                    return referenceDao.getEquipmentByIndexes(itemIndexes).map {
                        ChoiceOption(id = it.indexName, label = it.name, info = it.description)
                    }
                }
            }
        }

        // Явные опции из массива `options` (fallback)
        return optionSet["options"]?.jsonArray?.mapNotNull { parseOption(it.jsonObject) } ?: emptyList()
    }

    private fun parseOption(optionJsonObj: JsonObject): ChoiceOption? {
        val item = optionJsonObj["item"]?.jsonObject
        val id = item?.get("index")?.jsonPrimitive?.content ?: optionJsonObj["string"]?.jsonPrimitive?.content ?: return null
        val label = item?.get("name")?.jsonPrimitive?.content ?: DndLocalization.translateSkill(id)
        val desc = optionJsonObj["desc"]?.jsonPrimitive?.content
        return ChoiceOption(id, label, desc)
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