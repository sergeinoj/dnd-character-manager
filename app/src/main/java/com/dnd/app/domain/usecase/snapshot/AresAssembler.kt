// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\AresAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import android.util.Log
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.snapshot.*
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max


@Singleton
class AresAssembler @Inject constructor(
    private val calculator: DndCalculator,
    private val mechanicsProcessor: FeatureMechanicsProcessor,
    private val json: Json
) {
    private val TAG = "AresAssembler"

    fun assemble(
        statRegistry: StatRegistry,
        inventory: List<InventoryItemSnapshot>,
        magic: MagicalRegistrySnapshot,
        profBonus: Int,
        proficiencies: Map<String, Int>,
        totalCharLevel: Int,
        damageTypeMap: Map<String, String>,
        equippedIds: Set<String>,
        resourcePools: List<ResourcePoolSnapshot>,
        liveStateCharges: Map<String, Int>,
        innateUsage: Map<String, Int>,
        activeFeatures: List<Feature>,
        progressionRows: List<ProgressionEntity>,
        classLevels: Map<String, Int>,
        activeEffects: Set<String>,
        familiarRecord: com.dnd.app.domain.model.MonsterRecord? = null
    ): List<CombatAction> {
        val sMod = statRegistry.modifiers["STR"] ?: 0
        val dMod = statRegistry.modifiers["DEX"] ?: 0


        val allMechanics = mechanicsProcessor.process(activeFeatures, progressionRows, totalCharLevel, classLevels)
        val familiarAttack = familiarRecord?.actions?.firstOrNull { it.attackBonus != null || it.damage.isNotEmpty() }
        if (familiarRecord != null) {
            if (familiarAttack != null) {
                val damagePreview = if (familiarAttack.damage.isNotEmpty()) {
                    familiarAttack.damage.joinToString(" + ") { dmg ->
                        val typeLabel = dmg.type?.let { damageTypeMap[it] ?: it } ?: "Effect"
                        "${dmg.dice} ($typeLabel)"
                    }
                } else {
                    "\u2014"
                }
                Log.d(
                    TAG,
                    "Familiar attack resolved: index=${familiarRecord.index} name=${familiarRecord.name} " +
                        "action=${familiarAttack.name} attackBonus=${familiarAttack.attackBonus} " +
                        "damage=$damagePreview range=${familiarAttack.range}"
                )
            } else {
                Log.w(
                    TAG,
                    "Familiar has no attack action: index=${familiarRecord.index} name=${familiarRecord.name}"
                )
            }
        }


        val passiveBonuses = allMechanics.filter {
            it.type == MechanicType.PASSIVE_BONUS && (it.effectId == null || it.effectId in activeEffects)
        }
        val totalPassive = passiveBonuses.sumOf { it.scalingBonus }


        val rootActions = mutableMapOf<String, CombatAction>()
        val damageModifiers = allMechanics.filter { it.type == MechanicType.MODIFIER_STACK }
        val ammoTagToRefId = mapOf(
            "ammo_arrow" to "arrow",
            "ammo_bolt" to "crossbow-bolt",
            "ammo_bullet" to "sling-bullet",
            "ammo_needle" to "blowgun-needle"
        )
        val weaponToAmmoFallback = mapOf(
            "longbow" to "arrow",
            "shortbow" to "arrow",
            "crossbow-light" to "crossbow-bolt",
            "crossbow-heavy" to "crossbow-bolt",
            "crossbow-hand" to "crossbow-bolt",
            "sling" to "sling-bullet",
            "blowgun" to "blowgun-needle"
        )

        fun resolveAmmoRefId(weapon: InventoryItemSnapshot): String? {
            weapon.properties.forEach { propertyStr ->
                val match = Regex("""\[(ammo_[a-z0-9_]+)\]""").find(propertyStr)
                val tag = match?.groupValues?.getOrNull(1)
                if (tag != null) {
                    val mapped = ammoTagToRefId[tag]
                    if (!mapped.isNullOrBlank()) return mapped
                }
            }
            val ref = weapon.refId?.lowercase() ?: return null
            return weaponToAmmoFallback[ref]
        }

        fun modifierAppliesToTarget(mod: ResolvedMechanic, targetId: String?, allowGlobal: Boolean): Boolean {
            val parentId = mod.parentEffectId
            if (parentId.isNullOrBlank()) return allowGlobal
            if (targetId.isNullOrBlank()) return false
            return targetId.contains(parentId, ignoreCase = true) || parentId.contains(targetId, ignoreCase = true)
        }


        val propertyOverrides = allMechanics.filter {
            it.type == MechanicType.MODIFY_PROPERTY && it.dieSize > 0 &&
                    (it.id.contains("monk") || it.id.contains("unarmed") || it.id.contains("martial-arts"))
        }
        val bestUnarmed = propertyOverrides.maxByOrNull { it.dieSize }
        val unarmedDie = bestUnarmed?.dieSize?.let { "1d$it" }
        val canUseDexUnarmed = bestUnarmed?.statScaling?.contains("DEX") == true && dMod > sMod
        val unarmedAbMod = if (canUseDexUnarmed) dMod else sMod

        rootActions["unarmed_strike"] = CombatAction(
            uniqueId = "unarmed_strike",
            name = "\u0420\u0443\u043A\u043E\u043F\u0430\u0448\u043D\u0430\u044F \u0430\u0442\u0430\u043A\u0430",
            hitBonus = calculator.formatModifier(unarmedAbMod + profBonus),
            damageFormula = formatDamage(unarmedDie, unarmedAbMod + totalPassive, isUnarmed = true),
            damageType = damageTypeMap["bludgeoning"] ?: "\u0414\u0440\u043E\u0431\u044F\u0449\u0438\u0439",
            type = ActionType.WEAPON
        )


        val activeWeapons = inventory.filter { it.uniqueId in equippedIds && it.equipSlot == EquipSlot.WEAPON }
        val hasShield = inventory.any { it.uniqueId in equippedIds && it.equipSlot == EquipSlot.SHIELD }
        val isOffhandOccupied = hasShield || activeWeapons.size > 1

        activeWeapons.forEach { weapon ->
            val isCompatibleWithMonk = bestUnarmed != null && (weapon.category?.contains("simple") == true || weapon.refId == "shortsword")
            val weaponRefId = weapon.refId?.lowercase() ?: ""
            val isProf = proficiencies.containsKey(weaponRefId) ||
                    proficiencies.containsKey("weapon-$weaponRefId") ||
                    (weapon.category?.contains("simple") == true && proficiencies.containsKey("simple-weapons")) ||
                    (weapon.category?.contains("martial") == true && proficiencies.containsKey("martial-weapons")) ||
                    isCompatibleWithMonk

            val isFinesse = weapon.properties.any { it.contains("finesse", ignoreCase = true) }
            val isRanged = weapon.category?.contains("ranged", ignoreCase = true) == true
            val useDex = (isFinesse || isRanged || isCompatibleWithMonk) && dMod > sMod

            val abMod = if (useDex) dMod else sMod
            val totalHit = abMod + (if (isProf) profBonus else 0) + (weapon.hitBonus ?: 0) + weapon.magicBonusAttack

            var dmgDice = weapon.damage
            if (weapon.properties.any { it.contains("versatile") } && !isOffhandOccupied) {
                dmgDice = weapon.versatileDamage ?: dmgDice
            }
            if (isCompatibleWithMonk) dmgDice = compareDice(dmgDice, unarmedDie)

            var modifierString = ""

            val weaponTargetId = weapon.refId ?: weapon.uniqueId
            damageModifiers.filter { it.effectId in activeEffects }
                .filter { mod -> modifierAppliesToTarget(mod, weaponTargetId, allowGlobal = true) }
                .forEach { mod ->

                val apply = when (mod.effectId) {
                    DndConstants.MechanicKeys.EFFECT_SNEAK_ATTACK -> isFinesse || isRanged
                    DndConstants.MechanicKeys.EFFECT_RAGE -> !isRanged && !useDex
                    else -> true
                }

                if (apply) {
                    if (mod.effectId == DndConstants.MechanicKeys.EFFECT_RAGE) {
                        val rageBonus = parseFlatDamageBonus(mod.damageFormula)
                        if (rageBonus != 0) modifierString += " + $rageBonus"
                    } else if (mod.statScaling.isNotEmpty()) {
                        val statCode = mod.statScaling.first().uppercase()
                        val bonus = statRegistry.modifiers[statCode] ?: 0
                        val statLabel = DndLocalization.translateStat(statCode).take(3).uppercase()
                        if (bonus > 0) modifierString += " + $bonus ($statLabel)"
                    } else {
                        modifierString += " + ${mod.damageFormula}"
                    }
                }
            }

            val flatDmg = abMod + totalPassive + (weapon.damageBonus ?: 0) + weapon.magicBonusDamage
            val ammoRefId = resolveAmmoRefId(weapon)
            val ammoCount = ammoRefId?.let { refId ->
                inventory.asSequence()
                    .filter { it.containerId == null && it.refId?.equals(refId, ignoreCase = true) == true }
                    .sumOf { it.quantity }
            }

            rootActions["weapon_${weapon.uniqueId}"] = CombatAction(
                uniqueId = "weapon_${weapon.uniqueId}",
                name = weapon.name,
                hitBonus = calculator.formatModifier(totalHit),
                damageFormula = formatDamage(dmgDice, flatDmg) + modifierString,
                damageType = weapon.damageType?.let { damageTypeMap[it] ?: it } ?: "",
                type = ActionType.WEAPON,
                sourceUniqueId = weapon.uniqueId,
                quantity = if (weapon.properties.any { it.contains("thrown") }) weapon.quantity else null,
                ammoType = ammoRefId,
                currentCharges = ammoCount
            )
        }


        magic.sources.forEach { source ->
            val isRaceSource = source.sourceType == MagicSourceType.RACE
            val isItemSource = source.sourceType == MagicSourceType.ITEM
            val itemPool = if (isItemSource && source.exclusiveResourcePoolId != null) {
                resourcePools.find { it.id == source.exclusiveResourcePoolId }
            } else null
            val itemPoolId = itemPool?.id
            val itemCurrentCharges = itemPool?.let { pool ->
                val spent = liveStateCharges[pool.id] ?: 0
                max(0, pool.max - spent)
            }
            val itemMaxCharges = itemPool?.max
            val itemExhausted = isItemSource && itemPool != null && (itemCurrentCharges ?: 0) <= 0

            source.spells.forEach { spell ->
                if (spell.attackType != null || spell.damageMap.isNotEmpty() || spell.saveStat != null) {
                    val isSaveBased = spell.saveStat != null
                    val saveStatCode = spell.saveStat ?: "STR"
                    val translatedStat = DndLocalization.translateStat(saveStatCode).take(3).uppercase()

                    val dmgFormula = if (spell.level == 0) {
                        spell.damageMap.entries.filter { it.key <= totalCharLevel }.maxByOrNull { it.key }?.value
                    } else {
                        spell.damageMap[spell.level]
                    }

                    val innateUsed = if (isRaceSource) innateUsage[spell.id] ?: 0 else 0
                    var magicModifierString = ""
                    damageModifiers.filter { it.effectId in activeEffects }
                        .filter { mod -> modifierAppliesToTarget(mod, spell.id, allowGlobal = false) }
                        .forEach { mod ->
                            if (mod.statScaling.isNotEmpty()) {
                                val statCode = mod.statScaling.first().uppercase()
                                val bonus = statRegistry.modifiers[statCode] ?: 0
                                val statLabel = DndLocalization.translateStat(statCode).take(3).uppercase()
                                if (bonus > 0) magicModifierString += " + $bonus ($statLabel)"
                            } else {
                                magicModifierString += " + ${mod.damageFormula}"
                            }
                        }

            rootActions["spell_${source.sourceId}_${spell.id}"] = CombatAction(
                uniqueId = "spell_${source.sourceId}_${spell.id}",
                name = spell.name,
                hitBonus = if (isSaveBased) "" else calculator.formatModifier(source.attackBonus),
                saveDcInfo = if (isSaveBased) "\u0421\u041B ${source.saveDc} $translatedStat" else null,
                damageFormula = (dmgFormula ?: spell.damageDice ?: "\u2014") + magicModifierString,
                damageType = if (isSaveBased) "$translatedStat (\u0441\u043F\u0430\u0441)" else (spell.damageType?.let { damageTypeMap[it] ?: it } ?: "\u041C\u0430\u0433\u0438\u044F"),
                type = if (spell.level == 0) ActionType.CANTRIP else ActionType.SPELL,
                isSpell = true,
                isConcentration = spell.isConcentration,
                spellId = spell.id,
                level = spell.level,
                isRitual = spell.isRitual,
                sourceUniqueId = source.sourceId,
                damageMap = spell.damageMap,
                resourceId = itemPoolId,
                currentCharges = when {
                    isRaceSource -> max(0, 1 - innateUsed)
                    isItemSource -> itemCurrentCharges
                    else -> null
                },
                maxCharges = when {
                    isRaceSource -> 1
                    isItemSource -> itemMaxCharges
                    else -> null
                },
                isBlocked = (isRaceSource && innateUsed >= 1) || itemExhausted
            )
                }
            }
        }


        allMechanics.filter {
            (it.type == MechanicType.ADD_ACTION ||
                it.type == MechanicType.SCALING_ACTION ||
                it.isToggle ||
                it.type == MechanicType.MODIFIER_STACK) &&
                it.type != MechanicType.RIDER_EFFECT
        }.forEach { mech ->
            if (mech.conditions.contains("magic_source")) {
                val isSpellAction = mech.spellId != null || mech.actionType == ActionType.SPELL
                if (isSpellAction) return@forEach
            }
            val rawTag = mech.resourceId?.lowercase()
            val localizedTag = mech.resourceId?.let { DndLocalization.translateProficiency(it) }?.lowercase()
            val pool = resourcePools.find { it.id == mech.resourceId }
                ?: resourcePools.find { pool ->
                    val name = pool.name.lowercase()
                    val id = pool.id.lowercase()
                    val matchRaw = rawTag?.let { name.contains(it) || id.contains(it) } ?: false
                    val matchLocalized = localizedTag?.let { name.contains(it) || id.contains(it) } ?: false
                    matchRaw || matchLocalized
                }
            val resolvedResourceId = pool?.id ?: mech.resourceId
            val actionKey = "action_${mech.id}"

            val costInfo = when {
                mech.costType == CostType.VARIABLE -> "V"
                mech.costValue > 0 -> mech.costValue.toString()
                mech.isToggle -> "0"
                else -> null
            }

            val resolvedType = when {
                mech.id == "familiar-attack-reaction" -> ActionType.WEAPON
                mech.actionType != null -> mech.actionType
                mech.isToggle || mech.type == MechanicType.MODIFIER_STACK -> ActionType.FEATURE_TOGGLE
                else -> ActionType.ITEM
            }

            val familiarOverride = if (mech.id == "familiar-attack-reaction") familiarAttack else null
            val familiarDamage = familiarOverride?.damage ?: emptyList()
            val familiarBaseDamage = familiarDamage.firstOrNull()
            val familiarExtra = familiarDamage.drop(1)
            val familiarFormula = if (familiarBaseDamage != null) {
                val base = familiarBaseDamage.dice
                val extra = if (familiarExtra.isNotEmpty()) {
                    familiarExtra.joinToString(" + ") { dmg ->
                        val typeLabel = dmg.type?.let { damageTypeMap[it] ?: it } ?: "Effect"
                        "${dmg.dice} ($typeLabel)"
                    }
                } else ""
                if (extra.isBlank()) base else "$base + $extra"
            } else null

            val displayName = if (mech.id == "familiar-attack-reaction" && familiarOverride != null) {
                val familiarLabel = familiarRecord?.name ?: "\u0424\u0430\u043C\u0438\u043B\u044C\u044F\u0440"
                "${mech.name} - $familiarLabel: ${familiarOverride.name}"
            } else {
                mech.name
            }

            rootActions[actionKey] = CombatAction(
                uniqueId = actionKey,
                name = displayName,
                hitBonus = familiarOverride?.attackBonus?.let { calculator.formatModifier(it) } ?: (mech.hitBonus ?: ""),
                damageFormula = familiarFormula ?: if (mech.type == MechanicType.MODIFIER_STACK) (mech.damageFormula ?: "\u2014") else (mech.damageFormula ?: "\u2014"),
                damageType = familiarBaseDamage?.type?.let { damageTypeMap[it] ?: it } ?: (mech.damageType ?: "\u0421\u043F\u0435\u0446."),
                type = resolvedType,
                range = familiarOverride?.range ?: (mech.range ?: "5 \u0444\u0442."),
                resourceId = resolvedResourceId,
                isToggle = mech.isToggle,
                isActive = mech.effectId in activeEffects,
                effectId = mech.effectId,
                parentEffectId = mech.parentEffectId,
                isBlocked = mech.type != MechanicType.MODIFIER_STACK &&
                    mech.parentEffectId != null &&
                    mech.parentEffectId !in activeEffects,
                currentCharges = pool?.let { it.max - (liveStateCharges[it.id] ?: 0) },
                maxCharges = pool?.max,
                actionCostDescription = costInfo,
                description = mech.description,
                triggerDescriptions = buildTriggerDescriptions(mech.conditions, mech.effectId),
                nestedActions = mech.subActions.map { sub ->
                    CombatAction(
                        uniqueId = "${mech.id}_sub_${sub.id}",
                        name = sub.name, hitBonus = "",
                        damageFormula = sub.damageFormula ?: "\u2014",
                        damageType = sub.damageType?.let { damageTypeMap[it] ?: it } ?: "\u042D\u0444\u0444\u0435\u043A\u0442",
                        type = ActionType.ITEM, resourceId = mech.resourceId,
                        actionCostDescription = sub.costValue.toString(),
                        description = sub.description,
                        triggerDescriptions = buildSubActionTriggers(sub)
                    )
                }
            )
            if (mech.id == "eldritch-invocation-agonizing-blast") {
                val combat = rootActions[actionKey]
                Log.d(
                    TAG,
                    "AgonizingBlast action: mechType=${mech.type} effectId=${mech.effectId} parentEffectId=${mech.parentEffectId} isToggle=${mech.isToggle} blocked=${combat?.isBlocked} active=${combat?.isActive} activeEffects=${activeEffects}"
                )
            }
        }


        allMechanics.filter { it.type == MechanicType.RIDER_EFFECT }.forEach { rider ->
            val parentId = rider.parentEffectId ?: return@forEach

            val parent = rootActions.values.find {
                it.effectId == parentId ||
                    it.uniqueId.contains(parentId, ignoreCase = true) ||
                    parentId.contains(it.uniqueId, ignoreCase = true)
            }

            val rawTag = rider.resourceId?.lowercase()
            val localizedTag = rider.resourceId?.let { DndLocalization.translateProficiency(it) }?.lowercase()
            val riderPool = resourcePools.find { it.id == rider.resourceId }
                ?: resourcePools.find { pool ->
                    val name = pool.name.lowercase()
                    val id = pool.id.lowercase()
                    val matchRaw = rawTag?.let { name.contains(it) || id.contains(it) } ?: false
                    val matchLocalized = localizedTag?.let { name.contains(it) || id.contains(it) } ?: false
                    matchRaw || matchLocalized
                }
            val resolvedRiderResourceId = riderPool?.id ?: rider.resourceId

            val riderActive = rider.effectId in activeEffects
            val parentBlocked = parent?.let { it.isBlocked || (it.isToggle && !it.isActive) } ?: true

            val riderAction = CombatAction(
                uniqueId = "rider_${rider.id}",
                name = rider.name,
                hitBonus = "",
                damageFormula = rider.damageFormula ?: "\u2014",
                damageType = rider.damageType ?: "Effect",
                type = if (rider.isToggle) ActionType.FEATURE_TOGGLE else ActionType.ITEM,
                isToggle = rider.isToggle,
                isActive = riderActive,
                effectId = rider.effectId,
                parentEffectId = parentId,
                resourceId = resolvedRiderResourceId,
                isBlocked = parentBlocked,
                actionCostDescription = "0",
                description = rider.description,
                triggerDescriptions = buildTriggerDescriptions(rider.conditions, rider.effectId),
                nestedActions = rider.subActions.map { sub ->
                    CombatAction(
                        uniqueId = "${rider.id}_sub_${sub.id}",
                        name = sub.name,
                        hitBonus = "",
                        damageFormula = sub.damageFormula ?: "\u2014",
                        damageType = sub.damageType?.let { damageTypeMap[it] ?: it } ?: "Effect",
                        type = ActionType.ITEM,
                        parentEffectId = parentId,
                        isBlocked = parentBlocked || (rider.isToggle && !riderActive),
                        actionCostDescription = "0",
                        description = sub.description,
                        triggerDescriptions = buildSubActionTriggers(sub)
                    )
                }
            )

            if (parent != null) {
                val newNested = parent.nestedActions.toMutableList()
                newNested.add(riderAction)
                rootActions[parent.uniqueId] = parent.copy(nestedActions = newNested)
            } else {
                Log.w(TAG, "Rider without parent: id=${rider.id} parentEffectId=$parentId")
                rootActions["rider_${rider.id}"] = riderAction
            }
        }

        rootActions.values.firstOrNull { it.effectId == "effect_agonizing_blast" }?.let { action ->
            Log.d(
                TAG,
                "Final AgonizingBlast action: id=${action.uniqueId} type=${action.type} " +
                    "isToggle=${action.isToggle} isActive=${action.isActive} isBlocked=${action.isBlocked} " +
                    "parentEffectId=${action.parentEffectId} resourceId=${action.resourceId}"
            )
        }

        return rootActions.values
            .sortedWith(
                compareBy<CombatAction> { action ->
                    when {
                        action.uniqueId == "unarmed_strike" -> 100
                        action.type == ActionType.WEAPON -> 1
                        action.type == ActionType.CANTRIP -> 2
                        action.type == ActionType.SPELL -> 3
                        action.type == ActionType.ITEM -> 4
                        action.type == ActionType.FEATURE_TOGGLE -> 5
                        else -> 50
                    }
                }.thenBy { it.name }
            )
    }

    private fun compareDice(w: String?, m: String?): String? {
        if (m == null) return w
        if (w == null) return m
        val wS = Pattern.compile("d(\\d+)").matcher(w).let { if (it.find()) it.group(1)?.toInt() ?: 0 else 0 }
        val mS = Pattern.compile("d(\\d+)").matcher(m).let { if (it.find()) it.group(1)?.toInt() ?: 0 else 0 }
        return if (mS > wS) m else w
    }

    private fun formatDamage(dice: String?, bonus: Int, isUnarmed: Boolean = false): String {
        if (dice == null && bonus == 0) return if (isUnarmed) "1" else "\u2014"
        val base = dice?.replace("Рє", "d")


        if (base == null) {
            return (if (isUnarmed) (1 + bonus).toString() else bonus.toString())
        }

        val flatBase = base.toIntOrNull()
        if (flatBase != null) {
            val total = flatBase + bonus
            return (if (isUnarmed) max(1, total) else total).toString()
        }

        if (bonus == 0) return base
        val sign = if (bonus > 0) "+" else ""
        return "$base$sign$bonus"
    }

    private fun parseFlatDamageBonus(formula: String?): Int {
        if (formula.isNullOrBlank()) return 0
        return Regex("""[+-]?\d+""")
            .find(formula.replace(" ", ""))
            ?.value
            ?.toIntOrNull()
            ?: 0
    }

    private fun buildTriggerDescriptions(
        conditions: List<String>,
        effectId: String?
    ): List<String> {
        val triggers = mutableListOf<String>()
        conditions.filter { it.isNotBlank() }.forEach { cond ->
            triggers += "\u0423\u0441\u043B\u043E\u0432\u0438\u0435: ${cond.replace('_', ' ')}"
        }
        if (!effectId.isNullOrBlank()) {
            val clean = effectId.removePrefix("effect_").replace('_', ' ')
            triggers += "\u042D\u0444\u0444\u0435\u043A\u0442: $clean"
        }
        return triggers.distinct()
    }

    private fun buildSubActionTriggers(sub: SubAction): List<String> {
        val triggers = mutableListOf<String>()
        if (sub.costType == CostType.VARIABLE) triggers += "\u0422\u0440\u0438\u0433\u0433\u0435\u0440: \u043F\u0435\u0440\u0435\u043C\u0435\u043D\u043D\u0430\u044F \u0441\u0442\u043E\u0438\u043C\u043E\u0441\u0442\u044C"
        if (sub.scalingRatio > 1) triggers += "\u0422\u0440\u0438\u0433\u0433\u0435\u0440: \u043C\u0430\u0441\u0448\u0442\u0430\u0431\u0438\u0440\u043E\u0432\u0430\u043D\u0438\u0435 x${sub.scalingRatio}"
        return triggers
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\AresAssembler.kt

