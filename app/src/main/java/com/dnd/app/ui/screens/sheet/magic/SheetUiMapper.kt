package com.dnd.app.ui.screens.sheet.magic

import com.dnd.app.domain.model.magic.SlotPreference
import com.dnd.app.domain.model.magic.SpellCastContext
import com.dnd.app.domain.model.snapshot.*
import com.dnd.app.domain.usecase.GetConditionsUseCase
import com.dnd.app.domain.usecase.inventory.PriceCalculator
import com.dnd.app.ui.screens.sheet.BaseUiData
import com.dnd.app.ui.screens.sheet.ConditionUiModel
import com.dnd.app.ui.screens.sheet.DefenseTrait
import com.dnd.app.ui.screens.sheet.DisplayInventoryItem
import com.dnd.app.ui.screens.sheet.HitDicePool
import com.dnd.app.ui.screens.sheet.SenseTrait
import com.dnd.app.util.DndLocalization
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.RegexOption

@Singleton
class SheetUiMapper @Inject constructor(
    private val priceCalculator: PriceCalculator,
    private val buildAutoLoreUseCase: BuildAutoLoreUseCase
) {
    companion object {
        private const val WARLOCK_SOURCE_PREFIX = "class-warlock-"
        private val HIT_DICE_PATTERN = Regex("(\\d*)(?:[dD\\u043a\\u041a])(\\d+)", RegexOption.IGNORE_CASE)
    }

    private val weightFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun mapBaseData(char: SheetCharacter): BaseUiData {
        val snap = char.snapshot
        val live = char.liveState

        val bio = listOf(
            "\u041a\u043b\u0430\u0441\u0441" to snap.global.classTitle,
            "\u041c\u0438\u0440\u043e\u0432\u043e\u0437\u0437\u0440\u0435\u043d\u0438\u0435" to DndLocalization.translateAlignment(snap.global.alignment),
            "\u041f\u0440\u0435\u0434\u044b\u0441\u0442\u043e\u0440\u0438\u044f" to snap.global.backgroundName,
            "\u0411\u043e\u043d\u0443\u0441 \u043c\u0430\u0441\u0442\u0435\u0440\u0441\u0442\u0432\u0430" to (if (snap.proficiencyBonus >= 0) "+${snap.proficiencyBonus}" else "${snap.proficiencyBonus}"),
            "\u041f\u0430\u0441\u0441\u0438\u0432\u043d\u0430\u044f \u0432\u043d\u0438\u043c\u0430\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c" to calculatePassivePerception(snap).toString()
        ).filter { it.second.isNotBlank() }
        val manualBio = listOf(
            "Пол" to displayGender(snap.global.gender),
            "Черты характера" to snap.global.personalityTrait,
            "Идеалы" to snap.global.ideal,
            "Привязанности" to snap.global.bond,
            "Слабости" to snap.global.flaw,
            "Внешность" to snap.global.appearance,
            "Предыстория персонажа" to snap.global.backstory
        ).filter { it.second.isNotBlank() }

        val filteredCombatActions = snap.combatActions.filter { action ->
            when (action.type) {
                ActionType.ITEM -> action.sourceUniqueId == null || action.sourceUniqueId in live.equippedItemIds
                else -> true
            }
        }.sortedBy { it.name }

        val allContainerIds = snap.inventory.mapNotNull { it.containerId }.toSet()
        val fullInventory = snap.inventory
        val heroDefenseSummary = collectDefenseTraits(snap.features)
        val beastResistances = snap.transformationMonster?.damageResistances.orEmpty()
            .map { DefenseTrait(title = DndLocalization.translateDamageType(it), detail = null) }
        val beastImmunities = (
            snap.transformationMonster?.damageImmunities.orEmpty().map {
                DefenseTrait(title = DndLocalization.translateDamageType(it), detail = null)
            } + snap.transformationMonster?.conditionImmunities.orEmpty().map {
                DefenseTrait(title = DndLocalization.translateProficiency(it), detail = "\u0418\u043c\u043c\u0443\u043d\u0438\u0442\u0435\u0442 \u043a \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u044e")
            }
            ).distinctBy { it.title + (it.detail ?: "") }
        val transformedSpeedRaw = snap.transformationMonster?.speed?.get("walk")
            ?: snap.transformationMonster?.speed?.entries?.firstOrNull()?.value
        val transformedSpeed = transformedSpeedRaw
            ?.replace("feet", "", ignoreCase = true)
            ?.replace("foot", "", ignoreCase = true)
            ?.replace("ft", "", ignoreCase = true)
            ?.replace("\u0444\u0443\u0442\u043e\u0432", "", ignoreCase = true)
            ?.replace("\u0444\u0443\u0442", "", ignoreCase = true)
            ?.trim()
        val displaySpeed = if (!live.transformationId.isNullOrBlank() && !transformedSpeed.isNullOrBlank()) {
            transformedSpeed
        } else {
            snap.finalSpeed.toString()
        }

        return BaseUiData(
            name = snap.global.name,
            classTitle = snap.global.classTitle,
            level = snap.global.level,
            proficiencyBonus = if (snap.proficiencyBonus >= 0) "+${snap.proficiencyBonus}" else "${snap.proficiencyBonus}",
            passivePerception = calculatePassivePerception(snap).toString(),
            hpCurrent = live.hpCurrent,
            hpTemp = live.hpTemp,
            hpMax = snap.maxHp,
            transformationHp = live.transformationHp,
            deathSaves = live.deathSaves,
            initiative = snap.initiativeBonus,
            displayArmorClass = snap.finalArmorClass,
            displaySpeed = displaySpeed,
            formattedTotalWeight = weightFormatter.format(snap.totalWeight),
            maxCarryWeight = snap.maxCarryWeight,
            isEncumbered = snap.isEncumbered,
            coins = live.coins,
            weapons = mapInvList(fullInventory, live, SectionType.WEAPON, allContainerIds),
            armorAndShields = mapInvList(fullInventory, live, SectionType.ARMOR, allContainerIds),
            gear = mapInvList(fullInventory, live, SectionType.GEAR, allContainerIds),
            filteredCombatActions = filteredCombatActions,
            stats = snap.stats,
            skillsByStat = snap.skills.groupBy { it.statCode },
            bioFields = bio,
            manualBioFields = manualBio,
            autoLore = buildAutoLoreUseCase.build(snap),
            notes = live.notes,
            systemLogs = live.systemLogs,
            defenseResistances = heroDefenseSummary.resistances,
            defenseImmunities = heroDefenseSummary.immunities,
            heroDefenseResistances = heroDefenseSummary.resistances,
            heroDefenseImmunities = heroDefenseSummary.immunities,
            beastDefenseResistances = beastResistances,
            beastDefenseImmunities = beastImmunities,
            senses = collectSenseTraits(snap.features),
            languages = snap.languages,
            toolProficiencies = collectToolProficiencies(snap.proficiencies, snap.proficiencyLabels),
            proficiencies = snap.proficiencies,
            proficiencyLabels = snap.proficiencyLabels,
            hitDiceFormula = snap.hitDice,
            hitDicePools = parseHitDicePools(snap.hitDice),
            totalHitDice = snap.hitDiceCount,
            remainingHitDice = (snap.hitDiceCount - live.hitDiceSpent).coerceAtLeast(0),
            classResources = snap.resourcePools,
            resourceCharges = buildResourceCharges(snap, live),
            canWildShape = snap.canWildShape,
            isTransformed = !live.transformationId.isNullOrBlank(),
            transformationName = snap.transformationMonster?.name ?: live.transformationId,

            activeEffects = live.activeEffects,
            activeConditions = live.activeConditions,
            exhaustionLevel = live.exhaustionLevel,
            isConcentrating = !live.concentrationSpellId.isNullOrBlank(),
            concentrationSpellId = live.concentrationSpellId,
            familiar = snap.familiar,
            transformedMonster = snap.transformationMonster
        )
    }

    private fun collectToolProficiencies(
        proficiencies: Map<String, Int>,
        labels: Map<String, String>
    ): List<String> {
        return proficiencies.keys
            .asSequence()
            .filter { it.startsWith("tool-") }
            .map { id -> labels[id].orEmpty().ifBlank { DndLocalization.translateProficiency(id) } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    private enum class SectionType { WEAPON, ARMOR, GEAR }

    private fun displayGender(raw: String): String {
        return when (raw.trim().lowercase()) {
            "male", "m", "man", "мужчина" -> "Мужчина"
            "female", "f", "woman", "женщина" -> "Женщина"
            "other", "non-binary", "nb", "иное" -> "Иное"
            else -> raw
        }
    }

    private fun calculatePassivePerception(snap: CharacterSnapshot): Int {
        val perceptionMod = snap.skills
            .find {
                it.code.equals("perception", ignoreCase = true) ||
                    it.code.equals("skill-perception", ignoreCase = true)
            }
            ?.modifier
            ?.replace("+", "")
            ?.toIntOrNull()
            ?: 0
        val featureBonus = snap.features.sumOf { extractPassivePerceptionBonus(it.referenceJson) }
        return 10 + perceptionMod + featureBonus
    }

    private fun extractPassivePerceptionBonus(referenceJson: String?): Int {
        if (referenceJson.isNullOrBlank()) return 0
        val root = runCatching { json.parseToJsonElement(referenceJson) }.getOrNull() ?: return 0
        return collectPassivePerceptionBonus(root)
    }

    private fun collectPassivePerceptionBonus(node: JsonElement): Int {
        return when (node) {
            is JsonObject -> {
                var sum = 0
                node.forEach { (key, value) ->
                    val normalized = key.lowercase().replace("-", "_")
                    if (normalized == "passive_perception_bonus") {
                        sum += (value as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                    } else {
                        sum += collectPassivePerceptionBonus(value)
                    }
                }
                sum
            }
            is JsonArray -> node.sumOf { collectPassivePerceptionBonus(it) }
            else -> 0
        }
    }

    private fun mapInvList(
        inv: List<InventoryItemSnapshot>,
        live: CharacterLiveState,
        section: SectionType,
        containerIds: Set<String>
    ): List<DisplayInventoryItem> {
        return inv.filter { item ->
            when (section) {
                SectionType.WEAPON -> item.equipSlot == EquipSlot.WEAPON
                SectionType.ARMOR -> item.equipSlot == EquipSlot.ARMOR || item.equipSlot == EquipSlot.SHIELD
                SectionType.GEAR -> item.equipSlot !in listOf(EquipSlot.WEAPON, EquipSlot.ARMOR, EquipSlot.SHIELD)
            }
        }.map { i ->
            val currentQty = live.itemOverrides[i.uniqueId]?.quantity ?: i.quantity

            val canBeEquipped = i.equipSlot in listOf(
                EquipSlot.WEAPON, EquipSlot.ARMOR, EquipSlot.SHIELD, EquipSlot.ACCESSORY, EquipSlot.OTHER
            )

            DisplayInventoryItem(
                uniqueId = i.uniqueId,
                name = i.name,
                description = i.description,
                formattedWeight = weightFormatter.format(i.weight * currentQty),
                quantity = currentQty,
                canChangeQuantity = true,
                canBeEquipped = canBeEquipped,
                isEquipped = i.uniqueId in live.equippedItemIds,
                requiresAttunement = i.isAttunementRequired,
                isAttuned = i.uniqueId in live.attunedItemIds,
                isSellable = i.baseUnitCostCp > 0,
                sellPrice = priceCalculator.calculateSellPrice(i),
                rarity = DndLocalization.translateRarity(i.rarity),
                isPack = i.isPack || i.uniqueId in containerIds,
                containerId = i.containerId,
                isShield = i.equipSlot == EquipSlot.SHIELD
            )
        }.filter { it.quantity > 0 }
    }

    fun mapMagicData(char: SheetCharacter, prep: MagicPreparationDraft?, pending: Set<String>, isModified: Boolean): MagicUiState {
        val snap = char.snapshot
        val live = char.liveState
        val raceSpellIds = snap.magic?.sources
            ?.filter { it.sourceType == MagicSourceType.RACE }
            ?.flatMap { it.spells.map { spell -> spell.id } }
            ?.toSet() ?: emptySet()
        val allSources = snap.magic?.sources ?: emptyList()
        return MagicUiState(
            globalSlots = mapSlots(snap.magic, snap, live, pending),
            sources = allSources.map { mapSrc(it, snap, live, pending, raceSpellIds) },
            activePreparation = prep?.let { d ->
                allSources.find { it.sourceId == d.sourceId }?.let { s -> mapPrep(s, d, snap, live, pending, isModified) }
            },
            innateSpellIds = raceSpellIds
        )
    }

    private fun mapSlots(reg: MagicalRegistrySnapshot?, snap: CharacterSnapshot, live: CharacterLiveState, pending: Set<String>): GlobalSlotsUiModel? {
        val registry = reg ?: return null
        if (registry.globalSlots.isEmpty() && registry.pactMagic == null) return null
        val classSlots = buildSlotLevels(registry.globalSlots, live, pending)
        val innateSlots = buildInnateSlots(registry, live, pending)
        val pact = registry.pactMagic?.let { p ->
            val pendingPact = pending.count { it.startsWith("slot_pact_") }
            SpellSlotLevelUiModel(p.slotLevel, List(p.maxSlots) { i ->
                SpellSlotCircleUiModel(isSpent = i < live.spentPactSlots, isPending = i >= live.spentPactSlots && i < (live.spentPactSlots + pendingPact))
            })
        }
        val showClassSlots = !snap.isPurePactCaster && classSlots.isNotEmpty()
        if (!showClassSlots && registry.globalSlots.isEmpty() && pact == null) return null
        return GlobalSlotsUiModel(classSlots, innateSlots, pact, showClassSlots)
    }

    private fun buildSlotLevels(slots: Map<Int, Int>, live: CharacterLiveState, pending: Set<String>): List<SpellSlotLevelUiModel> {
        return slots.keys.sorted().map { lvl ->
            val max = slots[lvl] ?: 0
            val spent = live.spentGlobalSlots[lvl] ?: 0
            val pendingCount = pending.count { it.startsWith("slot_${lvl}_") }
            SpellSlotLevelUiModel(lvl, List(max) { i ->
                SpellSlotCircleUiModel(isSpent = i < spent, isPending = i >= spent && i < (spent + pendingCount))
            })
        }
    }

    private fun buildInnateSlots(registry: MagicalRegistrySnapshot, live: CharacterLiveState, pending: Set<String>): List<InnateSlotUiModel> {
        val slots = mutableListOf<InnateSlotUiModel>()
        registry.sources.filter { it.sourceType == MagicSourceType.RACE }.forEach { source ->
            source.spells.forEach { spell ->
                val slotId = "innate_${spell.id}_${slots.size}"
                val usedCount = live.innateUsage[spell.id] ?: 0
                val isPending = pending.contains(slotId)
                slots.add(
                    InnateSlotUiModel(
                        slotId = slotId,
                        spellId = spell.id,
                        spellName = spell.name,
                        usedCount = usedCount,
                        isSpent = usedCount > 0,
                        isPending = isPending,
                        action = CastAction.SpendInnateUsage(spell.id)
                    )
                )
            }
        }
        return slots
    }

    private fun mapSrc(
        source: MagicSourceSnapshot,
        snap: CharacterSnapshot,
        live: CharacterLiveState,
        pending: Set<String>,
        raceSpellIds: Set<String>
    ): SpellSourceUiModel {
        val preparedIds = live.preparedSpellIds[source.sourceId] ?: emptySet()
        val concentrationSpellId = live.concentrationSpellId
        val visibleBase = if (source.preparationMode == PreparationMode.PREPARED) {
            source.spells.filter { it.id in preparedIds || it.isAlwaysPrepared }
        } else {
            source.spells
        }
        val visible = if (source.sourceType == MagicSourceType.RACE) {
            visibleBase
        } else {
            visibleBase.filter { it.id !in raceSpellIds }
        }
        val groups = visible.groupBy { it.level }.entries.sortedBy { it.key }.map { (lvl, spells) ->
            SpellLevelGroup(if (lvl == 0) "\u0417\u0430\u0433\u043e\u0432\u043e\u0440\u044b" else "$lvl \u0443\u0440\u043e\u0432\u0435\u043d\u044c", spells.sortedBy { it.name }.map { mapSpell(it, source, snap.magic, snap, live, pending, concentrationSpellId) })
        }
        val poolId = source.exclusiveResourcePoolId
        val spent = poolId?.let { live.featureCharges[it] } ?: 0
        val maxCharges = if (poolId != null) snap.inventory.find { it.uniqueId == poolId }?.maxCharges ?: 0 else 0
        return SpellSourceUiModel(source.sourceId, source.displayName, "\u0421\u041b ${source.saveDc} | \u0411\u0410 ${source.attackBonus}", if (maxCharges == 0 && poolId != null) "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u043e: $spent" else null, poolId, maxCharges, spent, source.preparationMode == PreparationMode.PREPARED, groups)
    }

    private fun mapSpell(spell: SpellSnapshot, source: MagicSourceSnapshot, registry: MagicalRegistrySnapshot?, snap: CharacterSnapshot, live: CharacterLiveState, pending: Set<String>, concentrationSpellId: String? = null): SpellUiModel {
        val meta = resolveMeta(spell, source, registry, snap, live)
        val key = "cast_${source.sourceId}_${spell.id}"
        val tag = if (source.sourceType == MagicSourceType.RACE && registry?.raceDualSpellIds?.contains(spell.id) == true) {
            "\u041a\u0440\u043e\u0432\u043d\u043e\u0435/\u0414\u043e\u0433\u043e\u0432\u043e\u0440\u043d\u043e\u0435"
        } else null
        val isCurrentConcentration = spell.id == concentrationSpellId
        return SpellUiModel(
            id = spell.id,
            sourceId = source.sourceId,
            sourceTag = tag,
            name = spell.name,
            level = spell.level,
            school = spell.school,
            castingTime = spell.time,
            range = spell.range,
            components = spell.components,
            duration = spell.duration,
            description = spell.description,
            isConcentration = spell.isConcentration,
            isRitual = spell.isRitual,
            isAlwaysPrepared = spell.isAlwaysPrepared,
            isUpcast = meta.isUpcast,
            isResourceExhausted = meta.isExhausted,
            isPending = pending.contains(key),
            castAction = meta.action,
            castWarning = meta.warning,
            isWarlockSource = source.isWarlockSource(),
            isCurrentConcentration = isCurrentConcentration
        )
    }

    private fun resolveMeta(spell: SpellSnapshot, source: MagicSourceSnapshot, registry: MagicalRegistrySnapshot?, snap: CharacterSnapshot, live: CharacterLiveState): CastMeta {
        if (spell.level == 0) return CastMeta(null, null, false, false)
        if (spell.isFreeCast) {
            return CastMeta(CastAction.RitualIntent(spell.id), null, false, false)
        }
        if (source.sourceType == MagicSourceType.RACE) {
            val usedCount = live.innateUsage[spell.id] ?: 0
            return CastMeta(
                CastAction.SpendInnateUsage(spell.id),
                null,
                isUpcast = false,
                isExhausted = usedCount > 0
            )
        }
        if (source.sourceType == MagicSourceType.ITEM) {
            val poolId = source.exclusiveResourcePoolId
            return if (poolId != null) CastMeta(CastAction.SpendCharge(poolId), null, false, false) else CastMeta(null, "\u041d\u0435\u0442 \u0437\u0430\u0440\u044f\u0434\u043e\u0432", false, true)
        }
        if (registry == null) return CastMeta(null, null, false, true)
        val slotPreference = when {
            snap.isPurePactCaster -> SlotPreference.PACT_FIRST
            source.sourceType == MagicSourceType.RACE -> SlotPreference.GLOBAL_FIRST
            else -> SlotPreference.AUTO
        }
        val context = spell.toCastContext()
        if (!snap.isPurePactCaster && (live.spentGlobalSlots[spell.level] ?: 0) < (registry.globalSlots[spell.level] ?: 0)) {
            return CastMeta(CastAction.SpendSlot(spell.level, slotPreference, context), null, false, false)
        }
        val pact = registry.pactMagic
        if (pact != null && pact.slotLevel >= spell.level && live.spentPactSlots < pact.maxSlots) {
            return CastMeta(
                CastAction.SpendSlot(spell.level, slotPreference, context),
                if (pact.slotLevel > spell.level) "\u042f\u0447\u0435\u0439\u043a\u0430 \u043f\u0430\u043a\u0442\u0430" else null,
                false,
                false
            )
        }
        for (lvl in (spell.level + 1)..9) {
            if ((live.spentGlobalSlots[lvl] ?: 0) < (registry.globalSlots[lvl] ?: 0)) {
                return CastMeta(CastAction.SpendSlot(lvl, slotPreference, context), "\u0423\u0441\u0438\u043b\u0435\u043d\u0438\u0435 ($lvl \u0443\u0440.)", true, false)
            }
        }
        return CastMeta(if (spell.isRitual) CastAction.RitualIntent(spell.id) else null, "\u041d\u0435\u0442 \u044f\u0447\u0435\u0435\u043a", false, true)
    }

    private fun SpellSnapshot.toCastContext() = SpellCastContext(id, name, isConcentration)

    private data class CastMeta(val action: CastAction?, val warning: String?, val isUpcast: Boolean, val isExhausted: Boolean)

    private fun mapPrep(source: MagicSourceSnapshot, draft: MagicPreparationDraft, snap: CharacterSnapshot, live: CharacterLiveState, pending: Set<String>, isModified: Boolean): PreparationStateUiModel {
        val mandatory = source.spells.filter { it.isAlwaysPrepared }.map { it.id }.toSet()
        val voluntary = draft.selectedIds.count { it !in mandatory }
        val comp = compareBy<SpellSnapshot>({ it.level }, { it.name })
        val concentrationSpellId = live.concentrationSpellId
        return PreparationStateUiModel(
            source.sourceId,
            source.displayName,
            source.spells.filter { it.id in draft.selectedIds || it.isAlwaysPrepared }.sortedWith(comp).map { mapSpell(it, source, snap.magic, snap, live, pending, concentrationSpellId) },
            source.spells.filter { it.id !in draft.selectedIds && !it.isAlwaysPrepared }.sortedWith(comp).map { mapSpell(it, source, snap.magic, snap, live, pending, concentrationSpellId) },
            canLearnSpells = source.sourceId == "class-wizard",
            "$voluntary / ${source.maxPreparedSpells}",
            voluntary <= source.maxPreparedSpells && !pending.contains("prep_confirm") && isModified,
            isModified
        )
    }

    private fun buildResourceCharges(snap: CharacterSnapshot, live: CharacterLiveState): Map<String, Int> {
        val result = live.featureCharges.toMutableMap()
        val pactPoolName = DndLocalization.translateProficiency("Pact Slots")
        val pactPoolId = snap.resourcePools.firstOrNull { it.name.equals(pactPoolName, ignoreCase = true) }?.id
        if (pactPoolId != null) {
            result[pactPoolId] = live.spentPactSlots
        }
        return result
    }

    private fun MagicSourceSnapshot.isWarlockSource(): Boolean {
        return sourceId == "class-warlock" || sourceId.startsWith(WARLOCK_SOURCE_PREFIX)
    }

    private fun parseHitDicePools(formula: String): List<HitDicePool> {
        return HIT_DICE_PATTERN.findAll(formula).mapNotNull { match ->
            val dieType = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val count = match.groupValues[1].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 1
            HitDicePool(dieType, count)
        }.toList()
    }

    private fun collectDefenseTraits(features: List<FeatureDisplayModel>): DefenseSummary {
        val resistances = mutableListOf<DefenseTrait>()
        val immunities = mutableListOf<DefenseTrait>()

        features.forEach { feature ->
            val fromReference = extractDefenseFromReferenceJson(feature.referenceJson)
            if (fromReference.first.isEmpty() && fromReference.second.isEmpty()) return@forEach
            fromReference.first.forEach { resistances += DefenseTrait(title = it, detail = feature.name) }
            fromReference.second.forEach { immunities += DefenseTrait(title = it, detail = feature.name) }
        }

        return DefenseSummary(
            resistances.distinctBy { it.title + (it.detail ?: "") },
            immunities.distinctBy { it.title + (it.detail ?: "") }
        )
    }

    private fun extractDefenseFromReferenceJson(referenceJson: String?): Pair<List<String>, List<String>> {
        if (referenceJson.isNullOrBlank()) return emptyList<String>() to emptyList()
        val root = runCatching { json.parseToJsonElement(referenceJson) }.getOrNull() ?: return emptyList<String>() to emptyList()
        val resistances = mutableListOf<String>()
        val immunities = mutableListOf<String>()
        collectDefenseValues(root, resistances, immunities)
        return resistances.distinct() to immunities.distinct()
    }

    private fun collectDefenseValues(
        node: JsonElement,
        resistances: MutableList<String>,
        immunities: MutableList<String>
    ) {
        when (node) {
            is JsonObject -> {
                node.forEach { (key, value) ->
                    val lowerKey = key.lowercase()
                    when {
                        lowerKey.contains("resist") -> extractStringValues(value).forEach { resistances += DndLocalization.translateDamageType(it) }
                        lowerKey.contains("immun") -> extractStringValues(value).forEach { immunities += DndLocalization.translateProficiency(it) }
                        else -> collectDefenseValues(value, resistances, immunities)
                    }
                }
            }
            is JsonArray -> node.forEach { collectDefenseValues(it, resistances, immunities) }
            else -> Unit
        }
    }

    private fun extractStringValues(node: JsonElement): List<String> = when (node) {
        is JsonPrimitive -> listOfNotNull(node.content.takeIf { it.isNotBlank() })
        is JsonArray -> node.flatMap { extractStringValues(it) }
        is JsonObject -> {
            val direct = listOfNotNull(
                node["name"]?.let { (it as? JsonPrimitive)?.content },
                node["index"]?.let { (it as? JsonPrimitive)?.content },
                node["value"]?.let { (it as? JsonPrimitive)?.content }
            ).filter { it.isNotBlank() }
            if (direct.isNotEmpty()) direct else node.values.flatMap { extractStringValues(it) }
        }
    }

    private fun collectSenseTraits(features: List<FeatureDisplayModel>): List<SenseTrait> {
        val senseKeys = setOf("darkvision", "blindsight", "tremorsense", "truesight", "passive_perception")
        val out = mutableListOf<SenseTrait>()
        features.forEach { feature ->
            val featureId = feature.id.lowercase()
            val normalizedKey = when {
                featureId.contains("darkvision") -> "darkvision"
                featureId.contains("blindsight") -> "blindsight"
                featureId.contains("tremorsense") -> "tremorsense"
                featureId.contains("truesight") -> "truesight"
                featureId.contains("passive-perception") || featureId.contains("passive_perception") -> "passive_perception"
                else -> null
            }
            if (normalizedKey != null) out += SenseTrait(DndLocalization.translateSenseKey(normalizedKey), null)
            val root = feature.referenceJson?.takeIf { it.isNotBlank() }?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            if (root != null) {
                collectSenseKeys(root, senseKeys).forEach { key ->
                    out += SenseTrait(DndLocalization.translateSenseKey(key), null)
                }
                out += collectSenseTraitsFromMechanics(root, feature.name)
            }
        }
        return out.distinctBy { it.title + "|" + (it.detail ?: "") }
    }

    private fun collectSenseKeys(node: JsonElement, keys: Set<String>): Set<String> {
        val found = linkedSetOf<String>()
        when (node) {
            is JsonObject -> node.forEach { (key, value) ->
                val normalized = key.lowercase().replace('-', '_')
                if (normalized in keys) found += normalized
                if (normalized == "sense") {
                    val senseValue = (value as? JsonPrimitive)?.content
                    val mapped = senseValue?.let { normalizeSenseKey(it) }
                    if (mapped != null && mapped in keys) found += mapped
                }
                found += collectSenseKeys(value, keys)
            }
            is JsonArray -> node.forEach { found += collectSenseKeys(it, keys) }
            else -> Unit
        }
        return found
    }

    private fun collectSenseTraitsFromMechanics(node: JsonElement, featureName: String): List<SenseTrait> {
        val found = mutableListOf<SenseTrait>()
        when (node) {
            is JsonObject -> {
                val type = node["type"]?.let { (it as? JsonPrimitive)?.content }?.uppercase()
                if (type == "ADD_SENSE") {
                    val senseRaw = node["sense"]?.let { (it as? JsonPrimitive)?.content }
                    val range = node["range"]?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() }
                        ?: node["distance"]?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() }
                    val normalized = senseRaw?.let { normalizeSenseKey(it) }
                    val title = featureName.takeIf { it.isNotBlank() }
                        ?: normalized?.let { DndLocalization.translateSenseKey(it) }
                    if (title != null) {
                        val detail = range?.let { "$it ft" }
                        found += SenseTrait(title = title, detail = detail)
                    }
                }
                node.values.forEach { found += collectSenseTraitsFromMechanics(it, featureName) }
            }
            is JsonArray -> node.forEach { found += collectSenseTraitsFromMechanics(it, featureName) }
            else -> Unit
        }
        return found
    }

    private fun normalizeSenseKey(raw: String): String {
        return raw.lowercase().trim().replace('-', '_')
    }

    private data class DefenseSummary(val resistances: List<DefenseTrait>, val immunities: List<DefenseTrait>)
}
