// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ShopModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

enum class InventoryMode { STANDARD_PACKS, BUY_WITH_GOLD }
enum class ShopView { CATEGORIES, ITEMS }

data class ShopCategory(val index: String, val name: String)

data class ShopItem(
    val index: String,
    val name: String,
    val cost: Money,
    val weight: Double?,
    val description: String? = null
)

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


    fun toCopper(): Long {
        return (gp.toLong() * 100L) + (sp.toLong() * 10L) + cp.toLong()
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
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ShopModels.kt