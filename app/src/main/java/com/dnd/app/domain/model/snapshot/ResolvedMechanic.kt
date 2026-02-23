// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\ResolvedMechanic.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.snapshot


data class ResolvedMechanic(
    val id: String,
    val name: String,
    val type: MechanicType,
    val description: String? = null,
    val costType: CostType = CostType.FIXED,
    val costValue: Int = 0,


    val damageFormula: String? = null,
    val damageType: String? = null,
    val scalingBonus: Int = 0,
    val dieSize: Int = 0,
    val dieCount: Int = 0,


    val scalingRef: String? = null,


    val hitBonus: String? = null,
    val range: String? = null,
    val statScaling: List<String> = emptyList(),
    val statFilter: List<String> = emptyList(),


    val effectId: String? = null,
    val parentEffectId: String? = null,
    val resourceId: String? = null,
    val isToggle: Boolean = false,
    val priority: Int = 100,
    val conditions: List<String> = emptyList(),
    val actionType: ActionType? = null,
    val spellId: String? = null,


    val subActions: List<SubAction> = emptyList()
)

enum class MechanicType {
    ADD_ACTION,
    MODIFY_PROPERTY,
    PASSIVE_BONUS,
    MODIFIER_STACK,
    FEATURE_TOGGLE,
    RIDER_EFFECT,
    SCALING_ACTION
}

enum class CostType {
    FIXED,
    VARIABLE
}


data class SubAction(
    val id: String,
    val name: String,
    val costType: CostType = CostType.FIXED,
    val costValue: Int = 0,
    val damageFormula: String? = null,
    val damageType: String? = null,
    val description: String? = null,
    val scalingRatio: Int = 1
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\ResolvedMechanic.kt