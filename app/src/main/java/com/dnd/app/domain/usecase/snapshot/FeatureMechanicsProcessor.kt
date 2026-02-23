// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\FeatureMechanicsProcessor.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import android.util.Log
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.snapshot.CostType
import com.dnd.app.domain.model.snapshot.MechanicType
import com.dnd.app.domain.model.snapshot.ResolvedMechanic
import com.dnd.app.domain.model.snapshot.SubAction
import com.dnd.app.domain.model.snapshot.ActionType
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FeatureMechanicsProcessor @Inject constructor(
    private val json: Json
) {
    private val TAG = "MechanicsProcessor"

    fun process(
        features: List<Feature>,
        progressionRows: List<ProgressionEntity>,
        totalLevel: Int,
        classLevels: Map<String, Int>
    ): List<ResolvedMechanic> {
        val results = mutableListOf<ResolvedMechanic>()


        val latestProgressionByClass = progressionRows
            .groupBy { it.classIndex }
            .mapNotNull { (_, rows) -> rows.maxByOrNull { it.level } }

        latestProgressionByClass.forEach { row ->
            if (row.scalingBonus > 0) {
                val effectKey = DndConstants.MechanicKeys.classActiveEffect(row.classIndex)
                results.add(ResolvedMechanic(
                    id = "prog_bonus_${row.classIndex}",
                    name = "Бонус прогрессии",
                    type = MechanicType.PASSIVE_BONUS,
                    description = null,
                    scalingBonus = row.scalingBonus,
                    statFilter = listOf("STR"),
                    effectId = effectKey
                ))
            }
            if (row.dieSize > 0) {
                results.add(ResolvedMechanic(
                    id = "prog_die_${row.classIndex}",
                    name = "Кость прогрессии",
                    type = MechanicType.MODIFY_PROPERTY,
                    description = null,
                    dieSize = row.dieSize,
                    dieCount = row.dieCount.coerceAtLeast(1),
                    statScaling = listOf("STR", "DEX")
                ))
            }
        }


        features.forEach { feature ->
            val rawJson = feature.referenceJson ?: return@forEach
            val classIdx = feature.classIndex ?: "general"

            runCatching {
                val root = json.parseToJsonElement(rawJson).jsonObject
                val contextLevel = feature.classIndex?.let { classLevels[it] } ?: totalLevel

                val mechanicObject = root["mechanics"]?.jsonObject

                if (mechanicObject != null) {
                    val subActionsJson = root["sub_actions_json"] as? JsonArray
                    parseMechanicObject(feature, mechanicObject, subActionsJson, contextLevel, classIdx, root, results)
                } else {
                    parseLegacyReferenceMechanics(feature, root, results)
                    if (root["scaling_target"]?.jsonPrimitive?.content == "sneak_attack_dice") {
                        val dice = (contextLevel + 1) / 2
                        results.add(ResolvedMechanic(
                            id = feature.index, name = feature.name, type = MechanicType.MODIFIER_STACK,
                            description = feature.description,
                            damageFormula = "${dice}d6",
                            effectId = "effect_${feature.index}_active",
                            isToggle = true, dieSize = 0
                        ))
                    }
                }

            }.onFailure { e ->
                Log.e(TAG, "Dynamic resolution failed for ${feature.index}: ${e.message}")
            }
        }

        return results
    }

    private fun parseMechanicObject(
        feature: Feature,
        obj: JsonObject,
        subActionsArray: JsonArray?,
        contextLevel: Int,
        classIdx: String,
        root: JsonObject,
        results: MutableList<ResolvedMechanic>
    ) {
        val resourceTag = obj["resource_tag"]?.jsonPrimitive?.content
        val resourceIdExplicit = obj["resourceId"]?.jsonPrimitive?.content
        val resolvedResourceId = resolveResourceIdByTag(resourceTag, classIdx)
            ?: resourceTag
            ?: feature.index.takeIf { feature.maxCharges > 0 }
        val effectiveResourceId = resourceIdExplicit?.let { resolveResourceIdByTag(it, classIdx) ?: it }
            ?: resolvedResourceId

        val typeStr = obj["type"]?.jsonPrimitive?.content?.uppercase() ?: ""
        val rootConditions = root["conditions"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.contentOrNull
        } ?: emptyList()
        val objConditions = obj["conditions"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.contentOrNull
        } ?: emptyList()
        val baseConditions = (rootConditions + objConditions).distinct()
        val isMagicSource = root["magic_source"]?.jsonPrimitive?.booleanOrNull == true ||
            obj["magic_source"]?.jsonPrimitive?.booleanOrNull == true
        val mechanicConditions = if (isMagicSource) baseConditions + "magic_source" else baseConditions
        val synergy = (obj["synergy"] as? JsonObject) ?: (root["synergy"] as? JsonObject)
        val synergyTargetId = synergy?.get("target_id")?.jsonPrimitive?.content
            ?: obj["target_id"]?.jsonPrimitive?.content
        val synergyStat = synergy?.get("stat")?.jsonPrimitive?.content


        if (synergy != null && typeStr != "MODIFIER_STACK") {
            if (synergyTargetId != null && synergyStat != null) {

                val effectId = "effect_${feature.index}_synergy"
                results.add(ResolvedMechanic(
                    id = feature.index,
                    name = feature.name,
                    type = MechanicType.MODIFIER_STACK,
                    description = feature.description,
                    damageFormula = "+ $synergyStat",
                    effectId = effectId,
                    parentEffectId = synergyTargetId,
                    isToggle = true,
                    statScaling = listOf(synergyStat),
                    conditions = mechanicConditions
                ))
            }
        }


        if (typeStr.isBlank()) return

        val isRider = typeStr == "RIDER_EFFECT"

        val parsedSubActions = subActionsArray?.mapNotNull {
            parseSubAction(it.jsonObject, forceFree = isRider)
        } ?: emptyList()

        val scalingRef = obj["scaling_ref"]?.jsonPrimitive?.content
        val costValue = obj["cost"]?.jsonPrimitive?.intOrNull
            ?: obj["min_cost"]?.jsonPrimitive?.intOrNull
            ?: 0

        when (typeStr) {
            "SNEAK_ATTACK" -> {
                val dice = (contextLevel + 1) / 2
                results.add(ResolvedMechanic(
                    id = feature.index, name = feature.name, type = MechanicType.MODIFIER_STACK,
                    description = feature.description,
                    damageFormula = "${dice}d6",
                    effectId = obj["effect_id"]?.jsonPrimitive?.content ?: "effect_${feature.index}_active",
                    isToggle = true, scalingRef = scalingRef, dieSize = 0,
                    costValue = costValue
                ))
            }
            "MODIFIER_STACK" -> {
                val effectId = obj["effect_id"]?.jsonPrimitive?.content ?: "effect_${feature.index}_modifier"
                val statKeys = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
                val scalingStat = scalingRef?.uppercase()?.takeIf { it in statKeys }
                val baseStatScaling = when {
                    synergyStat != null -> listOf(synergyStat)
                    scalingStat != null -> listOf(scalingStat)
                    else -> emptyList()
                }
                val isRageEffect = effectId == DndConstants.MechanicKeys.EFFECT_RAGE
                val statScaling = if (isRageEffect) emptyList() else baseStatScaling
                val isSneakAttack = root["scaling_target"]?.jsonPrimitive?.content == "sneak_attack_dice" ||
                    (scalingRef?.contains("sneak_attack", ignoreCase = true) == true)
                val dice = if (isSneakAttack) (contextLevel + 1) / 2 else 0
                val damageFormula = when {
                    statScaling.isNotEmpty() -> "+ ${statScaling.first()}"
                    isSneakAttack -> "${dice}d6"
                    else -> obj["damage_formula"]?.jsonPrimitive?.content
                }

                results.add(ResolvedMechanic(
                    id = feature.index, name = feature.name, type = MechanicType.MODIFIER_STACK,
                    description = feature.description,
                    damageFormula = damageFormula, effectId = effectId,
                    isToggle = true, scalingRef = scalingRef, dieSize = 0,
                    conditions = mechanicConditions,
                    parentEffectId = synergyTargetId,
                    statScaling = statScaling,
                    costValue = costValue
                ))
                if (feature.index == "eldritch-invocation-agonizing-blast") {
                    Log.d(
                        TAG,
                        "AgonizingBlast mech: effectId=$effectId parentEffectId=$synergyTargetId statScaling=$statScaling cost=$costValue"
                    )
                }
            }
            "FEATURE_TOGGLE", "TOGGLE_FEATURE" -> {
                val finalEffectId = obj["effect_id"]?.jsonPrimitive?.content ?: "effect_${feature.index}_active"

                results.add(ResolvedMechanic(
                    id = feature.index, name = feature.name, type = MechanicType.FEATURE_TOGGLE,
                    description = feature.description,
                    effectId = finalEffectId,
                    isToggle = true,
                    resourceId = effectiveResourceId,
                    costValue = costValue,
                    conditions = mechanicConditions
                ))
            }
            "ADD_ACTION", "SCALING_ACTION" -> {
                val costType = if (obj["cost_type"]?.jsonPrimitive?.content == "VARIABLE") CostType.VARIABLE else CostType.FIXED
                val attackObj = obj["attack"] as? JsonObject
                val explicitDamage = attackObj?.get("damage")?.jsonPrimitive?.content
                    ?: attackObj?.get("damage_formula")?.jsonPrimitive?.content
                    ?: obj["damage"]?.jsonPrimitive?.content
                    ?: obj["damage_formula"]?.jsonPrimitive?.content
                val formula = attackObj?.get("formula")?.jsonPrimitive?.content
                    ?: obj["formula"]?.jsonPrimitive?.content
                val die = attackObj?.get("die")?.jsonPrimitive?.content
                    ?: obj["die"]?.jsonPrimitive?.content
                val finalDamageFormula = when {
                    !explicitDamage.isNullOrBlank() -> explicitDamage
                    !formula.isNullOrBlank() && die != null -> {
                        val diceCount = resolveFormula(formula, contextLevel)
                        "${diceCount}$die"
                    }
                    else -> null
                }


                val isFreePrimitive = obj["is_free"]?.jsonPrimitive
                val isFree = isFreePrimitive?.booleanOrNull == true || isFreePrimitive?.content == "true"

                val effectId = if (isFree) "effect_${feature.index}_active" else null
                val actionName = obj["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: feature.name
                val hitBonus = attackObj?.get("hit_bonus")?.jsonPrimitive?.content
                    ?: attackObj?.get("hitBonus")?.jsonPrimitive?.content
                    ?: obj["hit_bonus"]?.jsonPrimitive?.content
                    ?: obj["hitBonus"]?.jsonPrimitive?.content
                val actionTypeRaw = attackObj?.get("action_type")?.jsonPrimitive?.content
                    ?: obj["action_type"]?.jsonPrimitive?.content
                val mechanicId = obj["id"]?.jsonPrimitive?.content ?: feature.index
                val actionType = when {
                    !actionTypeRaw.isNullOrBlank() -> parseActionType(actionTypeRaw)
                    mechanicId == "familiar-attack-reaction" -> ActionType.WEAPON
                    else -> null
                }
                val spellId = obj["spell_id"]?.jsonPrimitive?.content
                val range = attackObj?.get("range")?.jsonPrimitive?.content
                    ?: obj["range"]?.jsonPrimitive?.content
                    ?: "5 фт."
                val damageType = attackObj?.get("damage_type")?.jsonPrimitive?.content
                    ?: obj["damage_type"]?.jsonPrimitive?.content

                results.add(ResolvedMechanic(
                    id = mechanicId,
                    name = actionName,
                    type = MechanicType.ADD_ACTION,
                    description = feature.description,
                    costType = costType,
                    costValue = costValue,
                    damageFormula = finalDamageFormula,
                    damageType = damageType ?: "Спец.",
                    hitBonus = hitBonus,
                    resourceId = if (isFree) null else effectiveResourceId,
                    range = range,
                    parentEffectId = obj["effect_link"]?.jsonPrimitive?.content,
                    subActions = parsedSubActions,
                    isToggle = isFree,
                    effectId = effectId,
                    conditions = mechanicConditions,
                    actionType = actionType,
                    spellId = spellId
                ))
            }
            "RIDER_EFFECT" -> {

                val effectId = obj["effect_id"]?.jsonPrimitive?.content ?: "effect_${feature.index}_active"
                val parentEffectId = obj["parentEffectId"]?.jsonPrimitive?.content
                    ?: obj["parent_engine"]?.jsonPrimitive?.content

                val actionName = obj["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: feature.name
                results.add(ResolvedMechanic(
                    id = feature.index,
                    name = actionName,
                    type = MechanicType.RIDER_EFFECT,
                    description = feature.description,
                    resourceId = effectiveResourceId,
                    parentEffectId = parentEffectId,
                    subActions = parsedSubActions,
                    isToggle = true,
                    effectId = effectId,
                    costValue = costValue,
                    conditions = mechanicConditions
                ))
            }
        }
    }


    private fun parseSubAction(obj: JsonObject, forceFree: Boolean): SubAction? {
        return try {
            val id = obj["id"]?.jsonPrimitive?.content ?: return null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Действие"
            val costType = if (obj["cost_type"]?.jsonPrimitive?.content == "VARIABLE") CostType.VARIABLE else CostType.FIXED


            val costValue = if (forceFree) 0 else (obj["min_cost"]?.jsonPrimitive?.int ?: obj["cost"]?.jsonPrimitive?.int ?: 0)

            SubAction(
                id = id,
                name = name,
                costType = costType,
                costValue = costValue,
                damageFormula = obj["damage"]?.jsonPrimitive?.content,
                damageType = obj["damage_type"]?.jsonPrimitive?.content,
                scalingRatio = obj["scaling"]?.jsonObject?.get("point_to_hp_ratio")?.jsonPrimitive?.int ?: 1
            )
        } catch (e: Exception) { null }
    }

    private fun parseLegacyReferenceMechanics(
        feature: Feature,
        root: JsonObject,
        results: MutableList<ResolvedMechanic>
    ) {
        val conditions = parseRootConditions(root)
        parseLegacyAdvantage(feature, root, conditions, results)
        parseLegacySpeedBonus(feature, root, conditions, results)
        parseLegacyJumpReduction(feature, root, conditions, results)
    }

    private fun parseLegacyAdvantage(
        feature: Feature,
        root: JsonObject,
        conditions: List<String>,
        results: MutableList<ResolvedMechanic>
    ) {
        val targets = root["advantage"]?.toAdvantageTargets().orEmpty()
        if (targets.isEmpty()) return
        val label = targets.joinToString(", ")
        results.add(
            ResolvedMechanic(
                id = "${feature.index}_advantage",
                name = feature.name,
                type = MechanicType.MODIFIER_STACK,
                description = feature.description,
                damageFormula = "Advantage on $label",
                effectId = "effect_${feature.index}_advantage",
                isToggle = true,
                conditions = conditions
            )
        )
        Log.d(TAG, "Legacy advantage mechanic for ${feature.index}: $label")
    }

    private fun parseLegacySpeedBonus(
        feature: Feature,
        root: JsonObject,
        conditions: List<String>,
        results: MutableList<ResolvedMechanic>
    ) {
        val speedBonus = root["speed_bonus"]?.asIntOrNull() ?: return
        if (speedBonus == 0) return
        val label = if (speedBonus > 0) "+$speedBonus" else speedBonus.toString()
        results.add(
            ResolvedMechanic(
                id = "${feature.index}_speed",
                name = feature.name,
                type = MechanicType.MODIFIER_STACK,
                description = feature.description,
                damageFormula = "$label speed",
                damageType = "Speed",
                effectId = "effect_${feature.index}_speed",
                conditions = conditions
            )
        )
        Log.d(TAG, "Legacy speed bonus for ${feature.index}: $speedBonus")
    }

    private fun parseLegacyJumpReduction(
        feature: Feature,
        root: JsonObject,
        conditions: List<String>,
        results: MutableList<ResolvedMechanic>
    ) {
        val jumpReduction = root["jump_reduction"]?.asIntOrNull() ?: return
        if (jumpReduction == 0) return
        val label = if (jumpReduction > 0) "-$jumpReduction" else jumpReduction.toString()
        results.add(
            ResolvedMechanic(
                id = "${feature.index}_jump",
                name = feature.name,
                type = MechanicType.MODIFIER_STACK,
                description = feature.description,
                damageFormula = "$label jump",
                damageType = "Jump",
                effectId = "effect_${feature.index}_jump",
                conditions = conditions
            )
        )
        Log.d(TAG, "Legacy jump reduction for ${feature.index}: $jumpReduction")
    }

    private fun parseRootConditions(root: JsonObject): List<String> {
        return (root["conditions"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull
        } ?: emptyList()
    }

    private fun JsonElement?.toAdvantageTargets(): List<String> {
        return when (this) {
            null -> emptyList()
            is JsonArray -> this.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> when {
                this.booleanOrNull == true -> listOf("all rolls")
                this.contentOrNull != null -> listOf(this.contentOrNull!!)
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun JsonElement?.asIntOrNull(): Int? {
        return (this as? JsonPrimitive)?.intOrNull
            ?: (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
    }

    private fun resolveResourceIdByTag(tag: String?, classIdx: String): String? {
        if (tag == null) return null
        return when (tag.lowercase()) {
            "ki", "rage", "divinity", "action_surge", "superiority_dice", "sorcery_points" -> "class_res_1_$classIdx"
            "lay_on_hands", "lay_on_hands_pool" -> "class_res_1_$classIdx"
            else -> null
        }
    }

    private fun resolveFormula(expr: String, L: Int): Int {
        if (expr.contains("(L+1)/2")) return (L + 1) / 2
        return expr.replace("L", L.toString()).toIntOrNull() ?: 1
    }

    private fun parseActionType(raw: String?): ActionType? = when (raw?.lowercase()) {
        "weapon" -> ActionType.WEAPON
        "cantrip" -> ActionType.CANTRIP
        "spell" -> ActionType.SPELL
        "item" -> ActionType.ITEM
        "feature_toggle", "toggle" -> ActionType.FEATURE_TOGGLE
        else -> null
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\FeatureMechanicsProcessor.kt
