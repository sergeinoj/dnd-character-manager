// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\PriceCalculator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.snapshot.InventoryItemSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor


@Singleton
class PriceCalculator @Inject constructor() {


    fun calculateSellPrice(item: InventoryItemSnapshot): Money {

        if (item.baseUnitCostCp <= 0) return Money()

        val sellValueCp = floor(item.baseUnitCostCp * 0.5).toInt()
        return Money.fromCp(sellValueCp)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\PriceCalculator.kt