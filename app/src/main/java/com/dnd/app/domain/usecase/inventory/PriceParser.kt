// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\PriceParser.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceParser @Inject constructor() {

    fun parse(costString: String): Int {
        val trimmed = costString.trim().lowercase()
        if (trimmed.isBlank()) return 0


        val plainNumber = trimmed.toIntOrNull()
        if (plainNumber != null) return plainNumber

        val parts = trimmed.split(" ")
        val value = parts.getOrNull(0)?.toIntOrNull() ?: return 0

        return when (parts.getOrNull(1)) {
            "gp" -> value * 100
            "sp" -> value * 10
            "cp" -> value
            else -> value
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\PriceParser.kt