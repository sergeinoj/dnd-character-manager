// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\UnpackEquipmentUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UnpackEquipmentUseCase @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val json: Json
) {
    private val TAG = "DND_LOG_UNPACK_UC"

    private data class RichMetadata(
        val name: String,
        val description: String?,
        val statsInfo: String?,
        val rarity: String?
    )

    suspend operator fun invoke(
        initialIndexes: List<String>
    ): Map<String, EquipmentOptionDetails> = coroutineScope {
        if (initialIndexes.isEmpty()) return@coroutineScope emptyMap()

        Log.d(TAG, "Unpacking initiated for ${initialIndexes.size} root items.")

        val allKnownIndexes = mutableSetOf<String>()
        val bundleContentMap = mutableMapOf<String, List<Pair<String, Int>>>()
        val processingQueue = ArrayDeque(initialIndexes)


        while (processingQueue.isNotEmpty()) {
            val batchToFetch = processingQueue.toList().filter { it !in allKnownIndexes }
            processingQueue.clear()
            if (batchToFetch.isEmpty()) continue

            val entities = classFeatureRepository.getEquipmentByIndexes(batchToFetch)
            allKnownIndexes.addAll(batchToFetch)

            for (entity in entities) {
                if (!entity.contentsJson.isNullOrBlank()) {
                    try {
                        val contents = mutableListOf<Pair<String, Int>>()
                        val jsonElement = json.parseToJsonElement(entity.contentsJson)
                        if (jsonElement is JsonArray) {
                            jsonElement.forEach { element ->
                                if (element is JsonObject) {
                                    val itemElement = element["item"]
                                    val index = when (itemElement) {
                                        is JsonObject -> itemElement["index"]?.jsonPrimitive?.content
                                        else -> itemElement?.jsonPrimitive?.content
                                    }

                                    if (index != null) {
                                        val quantity = element["quantity"]?.jsonPrimitive?.int ?: 1
                                        contents.add(index to quantity)
                                        if (index !in allKnownIndexes) processingQueue.addLast(index)
                                    }
                                }
                            }
                        }
                        bundleContentMap[entity.indexName] = contents
                    } catch (e: Exception) {
                        Log.e(TAG, "Critical failure parsing contents for bundle ${entity.indexName}", e)
                    }
                }
            }
        }


        val finalUniqueIndexes = allKnownIndexes.toList()
        val weaponsDef = async { classFeatureRepository.getWeaponsByIndexes(finalUniqueIndexes) }
        val armorDef = async { classFeatureRepository.getArmorByIndexes(finalUniqueIndexes) }
        val equipDef = async { classFeatureRepository.getEquipmentByIndexes(finalUniqueIndexes) }

        val weapons = weaponsDef.await()
        val armor = armorDef.await()
        val equipment = equipDef.await()


        val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
        val damageTypeMap = if (damageTypeIndices.isNotEmpty()) {
            damageTypeIndices.mapNotNull { classFeatureRepository.getDamageTypeByIndex(it) }.associate { it.indexName to it.name }
        } else emptyMap()

        val globalMetaMap = mutableMapOf<String, RichMetadata>()

        weapons.forEach { w ->
            val stats = DndLocalization.formatWeaponInfo(w.damage, damageTypeMap[w.damageType] ?: w.damageType)
            globalMetaMap[w.indexName] = RichMetadata(w.name, w.description, stats, DndLocalization.translateRarity(w.rarity))
        }
        armor.forEach { a ->
            val stats = DndLocalization.formatArmorInfo(a.acBase)
            globalMetaMap[a.indexName] = RichMetadata(a.name, a.description, stats, DndLocalization.translateRarity(a.rarity))
        }
        equipment.forEach { e ->
            if (!globalMetaMap.containsKey(e.indexName)) {
                globalMetaMap[e.indexName] = RichMetadata(e.name, e.description, null, null)
            }
        }


        val allDetails = mutableMapOf<String, EquipmentOptionDetails>()

        finalUniqueIndexes.forEach { index ->
            val meta = globalMetaMap[index]
            val mainName = meta?.name ?: index.replace("-", " ").capitalizeFirst()

            val finalDescription = DndLocalization.assembleEnrichedDescription(
                rarity = meta?.rarity,
                stats = meta?.statsInfo,
                description = meta?.description
            ).takeIf { it.isNotBlank() }

            val contents = bundleContentMap[index]
            val contentNames = contents?.map { (itemIndex, quantity) ->
                val itemName = globalMetaMap[itemIndex]?.name ?: itemIndex.replace("-", " ").capitalizeFirst()
                if (quantity > 1) "$itemName x$quantity" else itemName
            } ?: emptyList()

            allDetails[index] = EquipmentOptionDetails(mainName, contentNames, finalDescription)
        }

        Log.d(TAG, "Operation complete. Resolved ${allDetails.size} metadata entries.")
        return@coroutineScope allDetails
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\UnpackEquipmentUseCase.kt