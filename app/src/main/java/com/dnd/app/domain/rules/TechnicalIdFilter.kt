// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\rules\TechnicalIdFilter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.rules

import java.util.Locale

object TechnicalIdFilter {
    private val STAT_IDS = setOf("str", "dex", "con", "int", "wis", "cha")
    private val CLASS_TOKENS = setOf(
        "wizard",
        "warlock",
        "sorcerer",
        "bard",
        "monk",
        "fighter",
        "rogue",
        "cleric",
        "druid",
        "paladin",
        "ranger",
        "barbarian",
        "artificer"
    )
    private val SPECIAL_TOKENS = setOf("asi", "feat")

    private val TECHNICAL_IDS: Set<String> = buildSet {
        addAll(STAT_IDS)
        addAll(CLASS_TOKENS)
        addAll(SPECIAL_TOKENS)
    }

    fun shouldSkip(id: String): Boolean {
        val normalized = id.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return true
        return normalized in TECHNICAL_IDS
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\rules\TechnicalIdFilter.kt