// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\SheetActionHandler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.magic

import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.domain.model.snapshot.DeathSavesState
import com.dnd.app.domain.model.snapshot.EquipSlot
import com.dnd.app.domain.model.snapshot.ItemOverride
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.usecase.inventory.ModifyInventoryUseCase
import com.dnd.app.domain.usecase.magic.ManagePreparedSpellsUseCase
import com.dnd.app.domain.usecase.magic.RestorationUseCase
import com.dnd.app.domain.usecase.magic.SpendHitDiceUseCase
import com.dnd.app.domain.usecase.magic.SpendSpellSlotUseCase
import com.dnd.app.domain.usecase.ConcentrationProtocol
import com.dnd.app.domain.usecase.snapshot.DamageProcessor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Singleton
class SheetActionHandler @Inject constructor(
    private val repository: CharacterRepository,
    private val spendSpellSlotUseCase: SpendSpellSlotUseCase,
    private val spendHitDiceUseCase: SpendHitDiceUseCase,
    private val managePreparedSpellsUseCase: ManagePreparedSpellsUseCase,
    private val restorationUseCase: RestorationUseCase,
    private val modifyInventoryUseCase: ModifyInventoryUseCase,
    private val concentrationProtocol: ConcentrationProtocol,
    private val damageProcessor: DamageProcessor
) {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val _concentrationAlerts = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val concentrationAlerts: SharedFlow<String> = _concentrationAlerts


    suspend fun handleCast(characterId: Long, action: CastAction): Result<Unit> {
        return when (action) {
            is CastAction.SpendSlot -> spendSpellSlotUseCase(
                characterId,
                action.level,
                action.preference,
                spellContext = action.spellContext
            )
            is CastAction.SpendInnateUsage -> {
                val result = spendSpellSlotUseCase(characterId, 0, innateSpellId = action.spellId)
                if (result.isSuccess) {
                    logInnateUsage(characterId, action.spellId)
                }
                result
            }
            is CastAction.SpendCharge -> repository.performAtomicMutation(characterId) { snap, live, _ ->
                val poolId = action.poolId
                val pool = snap.resourcePools.find { it.id == poolId }
                    ?: return@performAtomicMutation Result.failure(Exception("\u0420\u0435\u0441\u0443\u0440\u0441 '$poolId' \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
                val current = live.featureCharges[poolId] ?: 0
                if (current >= pool.max) return@performAtomicMutation Result.failure(Exception("\u0417\u0430\u0440\u044F\u0434\u044B \u0438\u0441\u0447\u0435\u0440\u043F\u0430\u043D\u044B."))
                val nextCharges = live.featureCharges.toMutableMap().apply { put(poolId, current + 1) }
                Result.success(live.copy(featureCharges = nextCharges) to Unit)
            }
            is CastAction.RitualIntent -> repository.performAtomicMutation(characterId) { snap, live, _ ->
                val spell = snap.magic?.sources?.flatMap { it.spells }?.find { it.id == action.spellId }
                val entry = "[${timeFormat.format(Date())}] \u0420\u0438\u0442\u0443\u0430\u043B: ${spell?.name ?: action.spellId}"
                val nextLogs = (live.systemLogs + entry).takeLast(10)
                Result.success(live.copy(systemLogs = nextLogs) to Unit)
            }
        }
    }


    private suspend fun logInnateUsage(characterId: Long, spellId: String) {
        repository.performAtomicMutation(characterId) { snap, live, _ ->
            val spell = snap.magic?.sources?.flatMap { it.spells }?.find { it.id == spellId }
            val entry = "[${timeFormat.format(Date())}] \u0418\u0441\u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u043D\u043E \u0432\u0440\u043E\u0436\u0434\u0435\u043D\u043D\u043E\u0435 \u0437\u0430\u043A\u043B\u0438\u043D\u0430\u043D\u0438\u0435: ${spell?.name ?: spellId}"
            val nextLogs = (live.systemLogs + entry).takeLast(10)
            Result.success(live.copy(systemLogs = nextLogs) to Unit)
        }
    }


    suspend fun toggleEffect(id: Long, effectId: String) = repository.performAtomicMutation(id) { snap, live, _ ->
        val currentEffects = live.activeEffects.toMutableSet()
        val isActivating = effectId !in currentEffects


        val action = snap.combatActions.find { it.effectId == effectId }
        var nextCharges = live.featureCharges

        if (isActivating) {
            val resourceId = action?.resourceId
            val costStr = action?.actionCostDescription ?: "0"



            if (costStr == "V") {
                currentEffects.add(effectId)
            } else {
                val parsedCost = costStr.toIntOrNull()
                val cost = when {
                    parsedCost != null -> parsedCost
                    else -> 0
                }
                if (resourceId != null && cost > 0) {
                    val pool = snap.resourcePools.find { it.id == resourceId }
                    if (pool != null) {
                        val spent = live.featureCharges[resourceId] ?: 0

                        if (spent + cost > pool.max) {
                            return@performAtomicMutation Result.failure(
                                Exception("\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0440\u0435\u0441\u0443\u0440\u0441\u0430 '${pool.name}' (\u0422\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044F: $cost)")
                            )
                        }
                        nextCharges = live.featureCharges.toMutableMap().apply {
                            put(resourceId, spent + cost)
                        }
                    }
                }
                currentEffects.add(effectId)
            }
        } else {
            currentEffects.remove(effectId)
        }

        Result.success(live.copy(activeEffects = currentEffects, featureCharges = nextCharges) to Unit)
    }

    suspend fun toggleEquip(id: Long, itemUid: String) = repository.performAtomicMutation(id) { snap, live, _ ->
        val item = snap.inventory.find { it.uniqueId == itemUid }
            ?: return@performAtomicMutation Result.failure(Exception("\u041F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))

        if (item.equipSlot == EquipSlot.NONE)
            return@performAtomicMutation Result.failure(Exception("\u042D\u0442\u043E\u0442 \u043F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435\u043B\u044C\u0437\u044F \u044D\u043A\u0438\u043F\u0438\u0440\u043E\u0432\u0430\u0442\u044C."))

        val equippedIds = live.equippedItemIds.toMutableSet()
        val isEnabling = itemUid !in equippedIds

        if (isEnabling) {
            if (item.equipSlot == EquipSlot.ARMOR) {
                equippedIds.removeIf { oldId ->
                    snap.inventory.find { it.uniqueId == oldId }?.equipSlot == EquipSlot.ARMOR
                }
            } else {
                val isTwoHanded = item.properties.any { it.contains("two-handed", ignoreCase = true) }
                val neededHands = if (isTwoHanded) 2 else 1

                val inHands = equippedIds.mapNotNull { eid -> snap.inventory.find { it.uniqueId == eid } }
                    .filter { it.equipSlot == EquipSlot.WEAPON || it.equipSlot == EquipSlot.SHIELD }.toMutableList()

                var occupied = inHands.fold(0) { acc, hItem ->
                    acc + if (hItem.properties.any { p -> p.contains("two-handed", ignoreCase = true) }) 2 else 1
                }

                while (occupied + neededHands > 2 && inHands.isNotEmpty()) {
                    val removed = inHands.removeAt(0)
                    equippedIds.remove(removed.uniqueId)
                    occupied -= (if (removed.properties.any { p -> p.contains("two-handed", ignoreCase = true) }) 2 else 1)
                }
            }
            equippedIds.add(itemUid)
        } else {
            equippedIds.remove(itemUid)
        }
        Result.success(live.copy(equippedItemIds = equippedIds) to Unit)
    }

    suspend fun toggleAttunement(id: Long, itemUid: String) = repository.performAtomicMutation(id) { snap, live, _ ->
        val item = snap.inventory.find { it.uniqueId == itemUid } ?: return@performAtomicMutation Result.failure(Exception("\u041F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
        if (!item.isAttunementRequired) return@performAtomicMutation Result.failure(Exception("\u041F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438."))
        val attuned = live.attunedItemIds.toMutableSet()
        if (itemUid !in attuned) {
            if (attuned.size >= 3) return@performAtomicMutation Result.failure(Exception("\u0412\u0441\u0435 3 \u044F\u0447\u0435\u0439\u043A\u0438 \u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 \u0437\u0430\u043D\u044F\u0442\u044B."))
            attuned.add(itemUid)
        } else attuned.remove(itemUid)
        Result.success(live.copy(attunedItemIds = attuned) to Unit)
    }

    suspend fun updateQuantity(id: Long, itemUid: String, delta: Int) = repository.performAtomicMutation(id) { snap, live, _ ->
        val item = snap.inventory.find { it.uniqueId == itemUid } ?: return@performAtomicMutation Result.failure(Exception("\u041F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
        val current = live.itemOverrides[itemUid]?.quantity ?: item.quantity
        val next = (current + delta).coerceAtLeast(0)
        val overrides = live.itemOverrides.toMutableMap()
        if (next == item.quantity) overrides.remove(itemUid) else overrides[itemUid] = ItemOverride(next)
        var nextEquipped = live.equippedItemIds; var nextAttuned = live.attunedItemIds
        if (next == 0) { nextEquipped = live.equippedItemIds - itemUid; nextAttuned = live.attunedItemIds - itemUid }
        Result.success(live.copy(itemOverrides = overrides, equippedItemIds = nextEquipped, attunedItemIds = nextAttuned) to Unit)
    }
    suspend fun spendAmmoByRefId(id: Long, ammoRefId: String, amount: Int = 1) =
        repository.performAtomicMutation(id) { snap, live, _ ->
            val candidates = snap.inventory
                .filter { it.containerId == null && it.refId?.equals(ammoRefId, ignoreCase = true) == true }
                .map { item ->
                    val current = live.itemOverrides[item.uniqueId]?.quantity ?: item.quantity
                    item to current
                }
                .filter { (_, current) -> current > 0 }

            val (item, current) = candidates.maxByOrNull { it.second }
                ?: return@performAtomicMutation Result.failure(Exception("\u0411\u043e\u0435\u043f\u0440\u0438\u043f\u0430\u0441\u044b \u0437\u0430\u043a\u043e\u043d\u0447\u0438\u043b\u0438\u0441\u044c."))

            val next = (current - amount).coerceAtLeast(0)
            val overrides = live.itemOverrides.toMutableMap()
            if (next == item.quantity) overrides.remove(item.uniqueId) else overrides[item.uniqueId] = ItemOverride(next)
            var nextEquipped = live.equippedItemIds
            var nextAttuned = live.attunedItemIds
            if (next == 0) {
                nextEquipped = live.equippedItemIds - item.uniqueId
                nextAttuned = live.attunedItemIds - item.uniqueId
            }
            Result.success(live.copy(itemOverrides = overrides, equippedItemIds = nextEquipped, attunedItemIds = nextAttuned) to Unit)
        }
    suspend fun updateHp(id: Long, delta: Int, isTemp: Boolean) = repository.performAtomicMutation(id) { snap, live, _ ->
        if (isTemp) Result.success(live.copy(hpTemp = abs(delta)) to Unit)
        else {
            var nextTemp = live.hpTemp
            var nextHp = live.hpCurrent
            var damageTaken = 0
            if (delta < 0) {
                val damage = abs(delta)
                val fromTemp = kotlin.math.min(nextTemp, damage)
                val remainingDamage = damage - fromTemp
                nextTemp -= fromTemp
                if (remainingDamage > 0) {
                    val outcome = damageProcessor.processDamage(
                        liveState = live.copy(hpTemp = nextTemp),
                        damage = remainingDamage,
                        sourceLabel = "sheet"
                    )
                    nextHp = outcome.liveState.hpCurrent.coerceIn(0, snap.maxHp)
                    damageTaken = remainingDamage
                    var nextLive = outcome.liveState.copy(hpCurrent = nextHp, hpTemp = nextTemp)
                    val (loggedLive, alert) = concentrationProtocol.handleDamage(snap, nextLive, damageTaken)
                    nextLive = loggedLive
                    if (alert != null) _concentrationAlerts.emit(alert)
                    val nextSaves = if (nextHp > 0) DeathSavesState() else nextLive.deathSaves
                    return@performAtomicMutation Result.success(nextLive.copy(deathSaves = nextSaves) to Unit)
                }
            } else {
                val isTransformed = !live.transformationId.isNullOrBlank()
                if (isTransformed) {
                    val beastMaxHp = (snap.transformationMonster?.hitPoints ?: live.transformationHp).coerceAtLeast(0)
                    val healedTransformation = (live.transformationHp + delta).coerceIn(0, beastMaxHp)
                    val nextLive = live.copy(transformationHp = healedTransformation, hpTemp = nextTemp)
                    return@performAtomicMutation Result.success(nextLive to Unit)
                }
                nextHp = (nextHp + delta).coerceIn(0, snap.maxHp)
            }

            var nextLive = live.copy(hpCurrent = nextHp, hpTemp = nextTemp)
            if (damageTaken > 0) {
                val (loggedLive, alert) = concentrationProtocol.handleDamage(snap, nextLive, damageTaken)
                nextLive = loggedLive
                if (alert != null) _concentrationAlerts.emit(alert)
            }

            val nextSaves = if (nextHp > 0) DeathSavesState() else nextLive.deathSaves
            Result.success(nextLive.copy(deathSaves = nextSaves) to Unit)
        }
    }

    suspend fun updateDeathSaves(id: Long, isSuccess: Boolean) = repository.performAtomicMutation(id) { _, live, _ ->
        val current = live.deathSaves
        val next = if (isSuccess) current.copy(successes = (current.successes + 1).coerceAtMost(3)) else current.copy(failures = (current.failures + 1).coerceAtMost(3))
        Result.success(live.copy(deathSaves = next) to Unit)
    }

    suspend fun updateMoney(id: Long, t: String, d: Int) = repository.performAtomicMutation(id) { _, live, _ ->
        val change = when(t.uppercase()) { "GP" -> Money(gp=abs(d)); "SP" -> Money(sp=abs(d)); "CP" -> Money(cp=abs(d)); else -> Money() }
        val nextMoney = if (d >= 0) live.coins + change else live.coins - change
        if (nextMoney.toCopper() < 0) Result.failure(Exception("\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0441\u0440\u0435\u0434\u0441\u0442\u0432.")) else Result.success(live.copy(coins = nextMoney) to Unit)
    }

    suspend fun updateNotes(id: Long, t: String) = repository.performAtomicMutation(id) { _, live, _ -> Result.success(live.copy(notes = t) to Unit) }
    suspend fun purchaseItem(id: Long, i: ShopItem) = modifyInventoryUseCase.buyItem(id, i.index)
    suspend fun addLootItem(id: Long, itemIndex: String) = modifyInventoryUseCase.buyItem(id, itemIndex, overridePriceCp = 0)
    suspend fun sellItem(id: Long, uid: String, quantity: Int) = modifyInventoryUseCase.sellItem(id, uid, quantity)
    suspend fun syncPreparation(id: Long, sid: String, ids: Set<String>) = managePreparedSpellsUseCase(id, sid, ids)
    suspend fun performRest(id: Long, isL: Boolean) = restorationUseCase(id, isL)
    suspend fun performDawnReset(id: Long) = restorationUseCase.performDawnReset(id)
    suspend fun spendHitDie(id: Long, dieType: Int) = spendHitDiceUseCase(id, dieType)

    suspend fun addCondition(id: Long, condition: String) = repository.performAtomicMutation(id) { _, live, _ ->
        val next = live.activeConditions + condition
        Result.success(live.copy(activeConditions = next) to Unit)
    }

    suspend fun removeCondition(id: Long, condition: String) = repository.performAtomicMutation(id) { _, live, _ ->
        val next = live.activeConditions - condition
        Result.success(live.copy(activeConditions = next) to Unit)
    }

    suspend fun increaseExhaustion(id: Long) = repository.performAtomicMutation(id) { _, live, _ ->
        val next = (live.exhaustionLevel + 1).coerceAtMost(6)
        Result.success(live.copy(exhaustionLevel = next) to Unit)
    }

    suspend fun decreaseExhaustion(id: Long) = repository.performAtomicMutation(id) { _, live, _ ->
        val next = (live.exhaustionLevel - 1).coerceAtLeast(0)
        Result.success(live.copy(exhaustionLevel = next) to Unit)
    }

    suspend fun resetConcentration(id: Long) = repository.performAtomicMutation(id) { _, live, _ ->
        Result.success(live.copy(concentrationSpellId = null) to Unit)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\SheetActionHandler.kt

