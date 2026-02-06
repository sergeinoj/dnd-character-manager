// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/DraftCharacter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import com.dnd.app.domain.rules.DndRules

data class ClassLevel(
    val classId: Int,
    val className: String,
    val classLevelIndex: Int,
    val hitDie: Int
)

data class DraftCharacter(
    val baseStr: Int = 8, val baseDex: Int = 8, val baseCon: Int = 8,
    val baseInt: Int = 8, val baseWis: Int = 8, val baseCha: Int = 8,
    val raceId: Int? = null,
    val raceName: String = "",
    val raceStats: Map<String, Int> = emptyMap(),
    val levels: List<ClassLevel> = emptyList(),
    val name: String = ""
) {
    val totalLevel: Int get() = levels.size
    val finalStr: Int get() = (baseStr + (raceStats["strength"] ?: 0)).coerceAtMost(20)
    val finalDex: Int get() = (baseDex + (raceStats["dexterity"] ?: 0)).coerceAtMost(20)
    val finalCon: Int get() = (baseCon + (raceStats["constitution"] ?: 0)).coerceAtMost(20)
    val finalInt: Int get() = (baseInt + (raceStats["intelligence"] ?: 0)).coerceAtMost(20)
    val finalWis: Int get() = (baseWis + (raceStats["wisdom"] ?: 0)).coerceAtMost(20)
    val finalCha: Int get() = (baseCha + (raceStats["charisma"] ?: 0)).coerceAtMost(20)
    val pointsSpent: Int get() = DndRules.getPointCost(baseStr) + DndRules.getPointCost(baseDex) + DndRules.getPointCost(baseCon) + DndRules.getPointCost(baseInt) + DndRules.getPointCost(baseWis) + DndRules.getPointCost(baseCha)
    val pointsRemaining: Int get() = DndRules.MAX_POINTS - pointsSpent
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/DraftCharacter.kt