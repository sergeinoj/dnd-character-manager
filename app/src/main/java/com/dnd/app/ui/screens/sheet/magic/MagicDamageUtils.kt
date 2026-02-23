// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/ui/screens/sheet/magic/MagicDamageUtils.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.magic

import com.dnd.app.domain.model.snapshot.CombatAction

fun calculateUpcastDamage(action: CombatAction, level: Int): String {
    if (level <= 0) return "Эффект по описанию"
    val damageFormula = action.damageMap[level]
        ?: action.damageMap[action.damageMap.keys.firstOrNull() ?: level]
    return damageFormula
        ?: action.damageFormula.takeUnless { it.isBlank() || it == "—" }
        ?: "Эффект по описанию"
}

// --- КОНЕЦ ФАЙЛА ---
