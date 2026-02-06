// Имя файла: app/src/main/java/com/dnd/app/domain/model/ShopModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

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

    operator fun times(multiplier: Int): Money {
        if (multiplier < 0) return Money()
        return fromCp(this.totalInCp * multiplier)
    }

    override fun compareTo(other: Money): Int {
        return this.totalInCp.compareTo(other.totalInCp)
    }

    override fun toString(): String {
        if (totalInCp == 0) return "0 мм"

        val parts = mutableListOf<String>()
        if (gp != 0) parts.add("$gp зм")
        if (sp != 0) parts.add("$sp см")
        if (cp != 0) parts.add("$cp мм")

        return parts.joinToString(", ")
    }

    companion object {
        fun fromCp(totalCp: Int): Money {
            val gp = totalCp / 100
            val sp = (totalCp % 100) / 10
            val cp = totalCp % 10
            return Money(gp, sp, cp)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ShopModels.kt