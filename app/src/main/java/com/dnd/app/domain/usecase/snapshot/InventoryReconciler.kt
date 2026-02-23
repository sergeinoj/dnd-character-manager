// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\InventoryReconciler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import com.dnd.app.domain.model.snapshot.StatModel
import com.dnd.app.domain.usecase.inventory.ItemFabricator
import com.dnd.app.domain.usecase.inventory.UnpackedItem
import javax.inject.Inject
import javax.inject.Singleton


data class InventoryReconcileResult(
    val items: List<InventoryItemSnapshot>,
    val weightReport: WeightReport
)

@Singleton
class InventoryReconciler @Inject constructor(
    private val itemFabricator: ItemFabricator,
    private val weightCalculator: CalculateWeightUseCase
) {


    suspend fun reconcile(
        unpacked: List<UnpackedItem>,
        oldSnapshot: CharacterSnapshot?,
        liveState: CharacterLiveState,
        stats: Map<String, StatModel>
    ): InventoryReconcileResult {


        val fabricated = unpacked.map { item ->
            itemFabricator.fabricate(
                item = item,
                statsMap = stats,
                uniqueId = item.uniqueId,
                traceKey = item.sourceKey,
                baseUnitCostCp = item.costCp,
                isStarting = true
            )
        }

        val currentDraftTraceKeys = fabricated.map { it.traceKey }.toSet()



        val persistentItems = oldSnapshot?.inventory?.filter { item ->
            item.traceKey !in currentDraftTraceKeys
        }?.map { item ->
            itemFabricator.reFabricate(item)
        } ?: emptyList()


        val mergedInventory = (fabricated + persistentItems)
            .map { item ->
                val override = liveState.itemOverrides[item.uniqueId]
                if (override != null) {
                    item.copy(quantity = override.quantity)
                } else {
                    item
                }
            }
            .filter { item ->
                item.quantity > 0
            }


        val strScore = stats["STR"]?.value ?: 10
        val weightReport = weightCalculator(
            inventory = mergedInventory,
            itemOverrides = liveState.itemOverrides,
            strengthScore = strScore
        )

        return InventoryReconcileResult(
            items = mergedInventory,
            weightReport = weightReport
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\InventoryReconciler.kt