// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\CalculateWeightUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import com.dnd.app.domain.model.snapshot.ItemOverride
import javax.inject.Inject
import javax.inject.Singleton

data class WeightReport(
    val totalWeight: Double,
    val maxCarryWeight: Int,
    val isEncumbered: Boolean,
    val speedPenalty: Int
)

@Singleton
class CalculateWeightUseCase @Inject constructor() {

    operator fun invoke(
        inventory: List<InventoryItemSnapshot>,
        itemOverrides: Map<String, ItemOverride>,
        strengthScore: Int
    ): WeightReport {






        val totalWeight = inventory.sumOf { item ->
            val quantity = itemOverrides[item.uniqueId]?.quantity ?: item.quantity
            item.weight * quantity
        }

        val maxCarryWeight = strengthScore * 15
        val isEncumbered = totalWeight > maxCarryWeight
        val speedPenalty = if (isEncumbered) 10 else 0

        return WeightReport(
            totalWeight = totalWeight,
            maxCarryWeight = maxCarryWeight,
            isEncumbered = isEncumbered,
            speedPenalty = speedPenalty
        )
    }


    fun getFullItemWeight(
        targetItem: InventoryItemSnapshot,
        fullInventory: List<InventoryItemSnapshot>
    ): Double {
        val selfWeight = targetItem.weight * targetItem.quantity
        val children = fullInventory.filter { it.containerId == targetItem.uniqueId }
        val childrenWeight = children.sumOf { getFullItemWeight(it, fullInventory) }
        return selfWeight + childrenWeight
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\CalculateWeightUseCase.kt