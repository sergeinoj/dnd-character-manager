// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\DndConstants.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

object DndConstants {

    const val DEFAULT_CLASS_INDEX = "general_class"

    object VirtualKeys {
        const val PREPARED_SPELLS_PREFIX = "virtual-prepared-spells"
        const val INITIAL_CANTRIPS = "virtual-initial-cantrips"
        const val INITIAL_SPELLS = "virtual-initial-spells"

        fun preparedSpellsForClass(classIndex: String) = "$PREPARED_SPELLS_PREFIX-$classIndex"

        fun initialProficiencyChoice(classIndex: String, i: Int) = "virtual-prof-choice-${classIndex}-$i"
        fun initialEquipmentChoice(classIndex: String, i: Int) = "virtual-equip-choice-${classIndex}-$i"
        fun levelUpCantripChoice(level: Int) = "virtual-cantrip-choice-level-$level"
        fun levelUpSpellChoice(level: Int) = "virtual-spell-choice-level-$level"

        const val AGGREGATED_SPELL_CHOICE = "aggregated-spell-choice"

        const val SAVING_THROW_PREFIX = "saving-throw-"
    }

    object MechanicKeys {
        const val EFFECT_RAGE = "effect_rage"
        const val EFFECT_SNEAK_ATTACK = "effect_sneak_attack"
        const val EFFECT_PREFIX = "effect_"


        fun classActiveEffect(classIndex: String): String {
            return when (classIndex.lowercase()) {
                "barbarian" -> EFFECT_RAGE
                else -> "${EFFECT_PREFIX}${classIndex}_active"
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\DndConstants.kt