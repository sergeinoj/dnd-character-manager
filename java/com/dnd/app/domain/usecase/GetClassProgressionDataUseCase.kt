// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetClassProgressionDataUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.PartitionedFeatures
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.GetFeaturesForLevelUseCase
import com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВАЯ МОДЕЛЬ - ЭТАП 4]
 * Контейнер для всех данных, связанных с прогрессией класса,
 * который возвращает GetClassProgressionDataUseCase.
 */
data class ClassProgressionData(
    val partitionedFeatures: PartitionedFeatures,
    val aggregatedSpellFeature: Feature?,
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails>
)

/**
 * [НОВЫЙ USE CASE - ЭТАП 4]
 * Оркестратор, который инкапсулирует всю сложную цепочку получения,
 * разделения, агрегации и распаковки данных для одного уровня класса.
 *
 * Цепочка вызовов:
 * 1. GetFeaturesForLevelUseCase -> получает "сырые" фичи уровня.
 * 2. PartitionClassFeaturesUseCase -> разделяет их на группы для UI.
 * 3. SpellChoiceAggregatorUseCase -> собирает все выборы магии в один блок.
 * 4. Внутренняя логика -> распаковывает бандлы снаряжения для UI.
 *
 * Возвращает единый объект ClassProgressionData, готовый для отображения в ViewModel.
 */
@Singleton
class GetClassProgressionDataUseCase @Inject constructor(
    private val getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase,
    private val partitionUseCase: PartitionClassFeaturesUseCase,
    private val spellAggregatorUseCase: SpellChoiceAggregatorUseCase,
    private val libraryRepository: LibraryRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "DND_LOG_CLASS_DATA_UC"

    suspend operator fun invoke(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int
    ): ClassProgressionData {
        // Шаг 1: Получаем все способности для уровня
        val featuresResult = getFeaturesForLevelUseCase(classIndex, level, subclassIndex, abilityModifier)

        // Шаг 2: Разделяем их на логические группы (для вкладок "Класс" и "Вещи")
        val partitioned = partitionUseCase(featuresResult)

        // Шаг 3: Собираем все опции выбора заклинаний в одну "виртуальную" способность
        val aggregatedSpellFeature = spellAggregatorUseCase(partitioned.classSkillFeatures)

        // Шаг 4: Распаковываем бандлы снаряжения для красивого отображения
        val unpackedEquipment = unpackAllEquipmentOptions(partitioned.inventoryChoiceFeatures)

        return ClassProgressionData(
            partitionedFeatures = partitioned,
            aggregatedSpellFeature = aggregatedSpellFeature,
            unpackedEquipmentOptions = unpackedEquipment
        )
    }

    /**
     * Внутренняя логика, перенесенная из ViewModel.
     * Распаковывает "бандлы" (например, "набор исследователя") в список конкретных предметов.
     */
    private suspend fun unpackAllEquipmentOptions(features: List<Feature>): Map<String, EquipmentOptionDetails> {
        val allOptionIndexes = features
            .flatMap { it.choices }
            .filterIsInstance<FeatureChoiceDomain.SelectOption>()
            .flatMap { it.options }
            .map { it.id }
            .distinct()
        if (allOptionIndexes.isEmpty()) return emptyMap()

        val equipmentEntities = libraryRepository.getEquipmentByIndexes(allOptionIndexes).associateBy { it.indexName }
        val unpackedDetails = mutableMapOf<String, EquipmentOptionDetails>()
        val allContentIndexes = mutableSetOf<String>()

        equipmentEntities.values.forEach { entity ->
            entity.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    contents.forEach { item ->
                        item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.let {
                            allContentIndexes.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Malformed contents_json for ${entity.indexName}")
                }
            }
        }

        val contentEntities = if (allContentIndexes.isNotEmpty()) {
            libraryRepository.getEquipmentByIndexes(allContentIndexes.toList()).associateBy { it.indexName }
        } else {
            emptyMap()
        }

        for (index in allOptionIndexes) {
            val entity = equipmentEntities[index] ?: continue
            val contentNames = mutableListOf<String>()
            entity.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    for (item in contents) {
                        val itemIndex = item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                        val itemName = contentEntities[itemIndex]?.name ?: itemIndex?.replaceFirstChar { it.uppercase() } ?: "Неизвестный предмет"
                        val quantity = item["quantity"]?.jsonPrimitive?.int ?: 1
                        val formattedName = if (quantity > 1) "${itemName} x${quantity}" else itemName
                        contentNames.add(formattedName)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Second pass malformed contents_json for ${entity.indexName}")
                }
            }
            unpackedDetails[index] = EquipmentOptionDetails(entity.name, contentNames)
        }
        return unpackedDetails
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetClassProgressionDataUseCase.kt