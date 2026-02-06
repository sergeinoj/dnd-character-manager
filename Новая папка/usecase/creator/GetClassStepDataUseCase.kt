// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/GetClassStepDataUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.creator

import android.util.Log
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.creator.ClassStepData
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.PartitionClassFeaturesUseCase
import com.dnd.app.domain.usecase.SpellChoiceAggregatorUseCase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ USE CASE]
 * Оркестратор для вкладки "Класс". Собирает все необходимые данные в одну модель ClassStepData,
 * которую ViewModel просто помещает в UiState.
 */
@Singleton
class GetClassStepDataUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val partitionUseCase: PartitionClassFeaturesUseCase,
    private val spellAggregator: SpellChoiceAggregatorUseCase,
    private val json: Json
) {
    private val TAG = "GetClassStepDataUC"

    suspend operator fun invoke(
        classIndex: String,
        subclassIndex: String?,
        abilityModifier: Int
    ): ClassStepData {
        val featuresResult = libraryRepository.getClassFeaturesForLevel(classIndex, 1, subclassIndex, abilityModifier)
        val partitioned = partitionUseCase(featuresResult)
        val aggregatedSpellFeature = spellAggregator(partitioned.classSkillFeatures)
        val availableSubclasses = libraryRepository.getSubclassesForClass(classIndex)
        return ClassStepData(
            classFeatures = partitioned.classSkillFeatures,
            inventoryChoiceFeatures = partitioned.inventoryChoiceFeatures,
            subclassChoiceFeature = partitioned.subclassChoiceFeature,
            aggregatedSpellFeature = aggregatedSpellFeature,
            availableSubclasses = availableSubclasses
        )
    }

    suspend fun unpackAllEquipmentOptions(features: List<Feature>): Map<String, com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails> {
        val allOptionIndexes = features
            .flatMap { it.choices }
            .filterIsInstance<FeatureChoiceDomain.SelectOption>()
            .flatMap { it.options }
            .map { it.id }
            .distinct()
        if (allOptionIndexes.isEmpty()) return emptyMap()

        val equipmentEntities = libraryRepository.getEquipmentByIndexes(allOptionIndexes).associateBy { it.indexName }
        val unpackedDetails = mutableMapOf<String, com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails>()

        // Рекурсивный сбор всех вложенных индексов
        val allContentIndexes = mutableSetOf<String>()
        val queue = ArrayDeque(allOptionIndexes)
        while(queue.isNotEmpty()){
            val currentIndex = queue.removeFirst()
            equipmentEntities[currentIndex]?.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    contents.forEach { item ->
                        item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.let {
                            if(allContentIndexes.add(it)) queue.addLast(it)
                        }
                    }
                } catch (e: Exception) { Log.w(TAG, "Malformed contents_json for ${currentIndex}") }
            }
        }
        val allNeededEntities = (allOptionIndexes + allContentIndexes).distinct()
        val contentEntities = libraryRepository.getEquipmentByIndexes(allNeededEntities).associateBy { it.indexName }

        for (index in allOptionIndexes) {
            val entity = contentEntities[index] ?: continue
            val contentNames = mutableListOf<String>()
            entity.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    for (item in contents) {
                        val itemIndex = item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                        val itemName = contentEntities[itemIndex]?.name ?: itemIndex ?: "Неизвестный предмет"
                        val quantity = item["quantity"]?.jsonPrimitive?.int ?: 1
                        val formattedName = if(quantity > 1) "${itemName} x${quantity}" else itemName
                        contentNames.add(formattedName)
                    }
                } catch (e: Exception) { /* ... */ }
            }
            unpackedDetails[index] = com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails(entity.name, contentNames)
        }
        return unpackedDetails
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/GetClassStepDataUseCase.kt