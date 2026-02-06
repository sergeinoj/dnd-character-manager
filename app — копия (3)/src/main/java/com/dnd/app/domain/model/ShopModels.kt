// Имя файла: app/src/main/java/com/dnd/app/domain/model/ShopModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/** Определяет режим работы вкладки инвентаря. */
enum class InventoryMode { STANDARD_PACKS, BUY_WITH_GOLD }

/** Определяет, что отображается в магазине: категории или предметы. */
enum class ShopView { CATEGORIES, ITEMS }

/** Модель для UI-представления категории в магазине. */
data class ShopCategory(val index: String, val name: String)

/** Модель для UI-представления товара в магазине. */
data class ShopItem(
    val index: String,
    val name: String,
    val cost: Money,
    val weight: Double?,
    val description: String? = null
)

/**
 * Модель для денег.
 * Все операции производятся в медных монетах для точности.
 */
@Serializable
data class Money(val gp: Int = 0, val sp: Int = 0, val cp: Int = 0) : Comparable<Money> {

    private val totalInCp: Int = gp * 100 + sp * 10 + cp

    operator fun plus(other: Money): Money {
        return fromCp(this.totalInCp + other.totalInCp)
    }

    operator fun minus(other: Money): Money {
        return fromCp(this.totalInCp - other.totalInCp)
    }

    override fun compareTo(other: Money): Int {
        return this.totalInCp.compareTo(other.totalInCp)
    }

    override fun toString(): String {
        return "ЗМ: $gp, СМ: $sp, ММ: $cp"
    }

    companion object {
        fun fromCp(totalCp: Int): Money {
            val remainingCp = abs(totalCp)
            val gp = remainingCp / 100
            val sp = (remainingCp % 100) / 10
            val cp = remainingCp % 10
            return Money(gp, sp, cp)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ShopModels.kt