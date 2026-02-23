// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\ModifierRegistry.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import androidx.compose.runtime.Immutable


@Immutable
data class ModifierRegistry(
    val acBonus: Int = 0,
    val saveBonus: Int = 0,
    val profBonusMod: Int = 0,


    val statOverrides: Map<String, Int> = emptyMap(),


    val statBonuses: Map<String, Int> = emptyMap(),


    val skillBonuses: Map<String, Int> = emptyMap(),


    val specialFlags: Set<String> = emptySet()
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\ModifierRegistry.kt