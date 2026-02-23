package com.dnd.app.domain.model.monster

import kotlinx.serialization.Serializable

@Serializable
enum class PatternLogic {
    AND,
    OR,
    XOR;

    companion object {
        fun from(raw: String?): PatternLogic {
            return values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AND
        }
    }
}

@Serializable
enum class AttackPatternEntryType {
    ACTION,
    PATTERN;

    companion object {
        fun from(raw: String?): AttackPatternEntryType {
            return values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTION
        }
    }
}

@Serializable
data class AttackPatternEntry(
    val type: AttackPatternEntryType,
    val reference: String,
    val count: Int = 1
)

@Serializable
data class AttackPattern(
    val slug: String,
    val logic: PatternLogic,
    val description: String?,
    val entries: List<AttackPatternEntry>
)
