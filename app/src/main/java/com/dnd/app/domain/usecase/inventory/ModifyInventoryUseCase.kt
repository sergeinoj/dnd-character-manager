// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\ModifyInventoryUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.snapshot.ItemOverride
import com.dnd.app.domain.model.snapshot.PurchasedItemRecord
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.usecase.snapshot.CalculateWeightUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModifyInventoryUseCase @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val unpackItemUseCase: UnpackItemUseCase,
    private val weightCalculator: CalculateWeightUseCase,
    private val priceCalculator: PriceCalculator
)   {
    companion object {
        private val AMMO_MAPPING = mapOf(
            "ammo_arrow" to ("arrow" to 20),
            "ammo_bolt" to ("crossbow-bolt" to 20),
            "ammo_bullet" to ("sling-bullet" to 20),
            "ammo_needle" to ("blowgun-needle" to 50)
        )


        private val WEAPON_TO_AMMO_FALLBACK = mapOf(
            "longbow" to ("arrow" to 20),
            "shortbow" to ("arrow" to 20),
            "crossbow-light" to ("crossbow-bolt" to 20),
            "crossbow-heavy" to ("crossbow-bolt" to 20),
            "crossbow-hand" to ("crossbow-bolt" to 20),
            "sling" to ("sling-bullet" to 20),
            "blowgun" to ("blowgun-needle" to 50)
        )
    }


    suspend fun buyItem(characterId: Long, itemIndex: String, overridePriceCp: Int? = null): Result<Unit> {
        return characterRepository.performAtomicMutation(characterId) { snapshot, liveState, _ ->



            val existingRecord = liveState.purchasedItems.find { it.refIndex == itemIndex }
            val existingInInventory = snapshot.inventory.find { it.refId == itemIndex && it.containerId == null }


            val transactionId = existingInInventory?.traceKey ?: existingRecord?.traceKey ?: UUID.randomUUID().toString()

            val itemsToBuy = try {
                val initialUnpacked = unpackItemUseCase(mapOf(transactionId to itemIndex))
                val additionalItems = mutableListOf<UnpackedItem>()




                initialUnpacked.forEach { item ->
                    var ammoFound = false


                    item.properties.forEach { propertyStr ->
                        val match = Regex("""\[(ammo_[a-z0-9_]+)\]""").find(propertyStr)
                        if (match != null) {
                            val tag = match.groupValues[1]
                            AMMO_MAPPING[tag]?.let { (ammoIndex, amount) ->
                                addAmmo(transactionId, ammoIndex, amount, additionalItems)
                                ammoFound = true
                            }
                        }
                    }


                    if (!ammoFound) {
                        WEAPON_TO_AMMO_FALLBACK[itemIndex]?.let { (ammoIndex, amount) ->
                             addAmmo(transactionId, ammoIndex, amount, additionalItems)
                        }
                    }
                }

                initialUnpacked + additionalItems
            } catch (e: Exception) {
                return@performAtomicMutation Result.failure(e)
            }

            if (itemsToBuy.isEmpty()) {
                return@performAtomicMutation Result.failure(ItemNotFoundException("Item not found in database."))
            }


            val totalCost = overridePriceCp?.toLong() ?: itemsToBuy.sumOf { it.costCp }.toLong()
            if (liveState.coins.toCopper() < totalCost) {
                return@performAtomicMutation Result.failure(NotEnoughGoldException())
            }


            val strScore = snapshot.statsMap["STR"]?.value ?: 10
            val currentWeightReport = weightCalculator(snapshot.inventory, liveState.itemOverrides, strScore)
            val incomingWeight = itemsToBuy.sumOf { it.weight }

            if (currentWeightReport.totalWeight + incomingWeight > strScore * 15) {
                return@performAtomicMutation Result.failure(OverEncumberedException())
            }


            val newOverrides = liveState.itemOverrides.toMutableMap()
            itemsToBuy.forEach { item ->
                val currentQty = newOverrides[item.uniqueId]?.quantity
                    ?: snapshot.inventory.find { it.uniqueId == item.uniqueId }?.quantity ?: 0
                newOverrides[item.uniqueId] = ItemOverride(currentQty + item.quantity)
            }


            val updatedPurchased = if (existingRecord == null) {
                liveState.purchasedItems + PurchasedItemRecord(
                    id = transactionId,
                    refIndex = itemIndex,
                    timestamp = System.currentTimeMillis(),
                    capturedPriceInCp = totalCost.toInt(),
                    traceKey = transactionId
                )
            } else {
                liveState.purchasedItems.map {
                    if (it.traceKey == transactionId) {
                        it.copy(capturedPriceInCp = it.capturedPriceInCp + totalCost.toInt())
                    } else it
                }
            }

            Result.success(liveState.copy(
                coins = liveState.coins - Money.fromCp(totalCost.toInt()),
                itemOverrides = newOverrides,
                purchasedItems = updatedPurchased
            ) to Unit)
        }
    }

    suspend fun sellItem(characterId: Long, itemUniqueId: String, quantity: Int): Result<Unit> {
        return characterRepository.performAtomicMutation(characterId) { snapshot, liveState, _ ->
            val itemToSell = snapshot.inventory.find { it.uniqueId == itemUniqueId }
                ?: return@performAtomicMutation Result.failure(ItemNotFoundException("Item not found."))

            val targetTraceKey = itemToSell.traceKey
            val unitSellPrice = priceCalculator.calculateSellPrice(itemToSell)
            val totalSellValueCp = unitSellPrice.toCopper() * quantity

            val newOverrides = liveState.itemOverrides.toMutableMap()
            val currentQty = newOverrides[itemToSell.uniqueId]?.quantity ?: itemToSell.quantity
            val nextQty = (currentQty - quantity).coerceAtLeast(0)

            if (nextQty == 0 && !itemToSell.isStartingEquipment) {
                newOverrides.remove(itemUniqueId)
            } else {
                newOverrides[itemUniqueId] = ItemOverride(quantity = nextQty)
            }


            var newEquippedIds = liveState.equippedItemIds
            var newAttunedIds = liveState.attunedItemIds
            if (nextQty == 0) {
                newEquippedIds = liveState.equippedItemIds - itemUniqueId
                newAttunedIds = liveState.attunedItemIds - itemUniqueId
            }


            val anyLeftInBundle = newOverrides.entries.any { (id, ovr) ->
                id.startsWith(targetTraceKey) && ovr.quantity > 0
            }

            val updatedPurchasedItems = if (!anyLeftInBundle) {
                liveState.purchasedItems.filterNot { it.traceKey == targetTraceKey }
            } else {
                liveState.purchasedItems
            }

            val nextLiveState = liveState.copy(
                coins = liveState.coins + Money.fromCp(totalSellValueCp.toInt()),
                itemOverrides = newOverrides,
                equippedItemIds = newEquippedIds,
                attunedItemIds = newAttunedIds,
                purchasedItems = updatedPurchasedItems
            )

            Result.success(nextLiveState to Unit)
        }
    }
    private suspend fun addAmmo(
        transactionId: String,
        ammoIndex: String,
        amount: Int,
        destination: MutableList<UnpackedItem>
    ) {
        val ammoTxId = "${transactionId}_auto_ammo_${UUID.randomUUID().toString().take(4)}"
        val ammoList = unpackItemUseCase(mapOf(ammoTxId to ammoIndex))
            .map { it.copy(quantity = amount) }
        destination.addAll(ammoList)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\ModifyInventoryUseCase.kt