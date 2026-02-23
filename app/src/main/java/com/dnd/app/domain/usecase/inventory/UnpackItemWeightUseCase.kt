// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\UnpackItemWeightUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UnpackItemWeightUseCase @Inject constructor(
    private val dao: ReferenceDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val maxDepth = 10
    private val TAG = "UnpackItemWeightUseCase"

    @Throws(ItemUnpackException::class)
    suspend operator fun invoke(itemIndex: String): Double {
        return calculateRecursiveWeight(itemIndex, emptySet(), 0)
    }

    private suspend fun calculateRecursiveWeight(
        currentIndex: String,
        path: Set<String>,
        depth: Int
    ): Double {
        if (depth > maxDepth) {
            val message = "Max recursion depth exceeded for item '$currentIndex'. Cycle detected or data is too deep."
            Log.e(TAG, message)
            throw ItemUnpackException(message)
        }
        if (currentIndex in path) {
            val message = "Cyclic dependency detected for item '$currentIndex'. Path: $path"
            Log.e(TAG, message)
            throw ItemUnpackException(message)
        }

        val newPath = path + currentIndex
        var currentWeight = 0.0

        val equipment = dao.getEquipmentByIndexes(listOf(currentIndex)).firstOrNull()
        if (equipment != null) {
            currentWeight += equipment.weight
            if (!equipment.contentsJson.isNullOrBlank()) {
                runCatching {
                    val element = json.parseToJsonElement(equipment.contentsJson)
                    if (element is JsonArray) {
                        element.forEach { el ->
                            if (el is JsonObject) {
                                val itemElement = el["item"]
                                val contentIndex = when (itemElement) {
                                    is JsonObject -> itemElement["index"]?.jsonPrimitive?.content
                                    else -> itemElement?.jsonPrimitive?.content
                                }

                                if (contentIndex != null) {
                                    val contentQuantity = el["quantity"]?.jsonPrimitive?.int ?: 1
                                    currentWeight += calculateRecursiveWeight(contentIndex, newPath, depth + 1) * contentQuantity
                                }
                            }
                        }
                    }
                }
            }
            return currentWeight
        }

        val weapon = dao.getWeaponsByIndexes(listOf(currentIndex)).firstOrNull()
        if (weapon != null) {
            return weapon.weight
        }

        val armor = dao.getArmorByIndexes(listOf(currentIndex)).firstOrNull()
        if (armor != null) {
            return armor.weight
        }

        return 0.0
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\UnpackItemWeightUseCase.kt