package com.dnd.app.ui.screens.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.calculator.DiceRoller
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.domain.model.magic.SlotPreference
import com.dnd.app.domain.model.magic.SpellCastContext
import com.dnd.app.domain.model.snapshot.ActionType
import com.dnd.app.domain.model.snapshot.CombatAction
import com.dnd.app.domain.model.snapshot.SheetCharacter
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.magic.LearnWizardSpellUseCase
import com.dnd.app.ui.screens.sheet.magic.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class CharacterSheetViewModel @Inject constructor(
    private val repository: CharacterRepository,
    private val libraryRepository: LibraryRepository,
    private val learnWizardSpellUseCase: LearnWizardSpellUseCase,
    private val uiMapper: SheetUiMapper,
    private val actionHandler: SheetActionHandler,
    private val diceRoller: DiceRoller,
    val merchantManager: MerchantManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: Long = savedStateHandle.get<Long>("characterId") ?: 0L

    private val _fatalError = MutableStateFlow<String?>(null)
    private val _prepDraft = MutableStateFlow<MagicPreparationDraft?>(null)
    private val _isShopOpen = MutableStateFlow(false)
    private val _interactionError = MutableStateFlow<String?>(null)
    private val _pendingActions = MutableStateFlow<Set<String>>(emptySet())
    private val _localNotes = MutableStateFlow<String?>(null)
    private val _rollResult = MutableStateFlow<String?>(null)
    private val _lootSearchQuery = MutableStateFlow("")
    private val _lootSearchResults = MutableStateFlow<List<ShopItem>>(emptyList())
    private val _activeTacticalAction = MutableStateFlow<CombatAction?>(null)
    private val _concentrationDialogMessage = MutableStateFlow<String?>(null)
    private val _showHitDiceDialog = MutableStateFlow(false)
    private val _hitDiceRemainingByType = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val _pendingHitDiceReset = MutableStateFlow(true)

    private var lootSearchJob: Job? = null
    private var notesJob: Job? = null
    private var errorJob: Job? = null
    private var lastChar: SheetCharacter? = null

    private val repositoryFlow = if (characterId != 0L) {
        repository.getCharacterForSheet(characterId)
            .onEach { lastChar = it }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    } else emptyFlow<SheetCharacter?>()

    private val uiDataFlow: StateFlow<CharacterSheetUiData?> = combine(
        repositoryFlow.filterNotNull().map { uiMapper.mapBaseData(it) }.distinctUntilChanged(),
        combine(repositoryFlow.filterNotNull(), _prepDraft, _pendingActions) { char, prep, pending ->
            val isModified = if (prep != null) {
                val original: Set<String> = char.liveState.preparedSpellIds[prep.sourceId] ?: emptySet()
                prep.selectedIds != original
            } else false
            uiMapper.mapMagicData(char, prep, pending, isModified)
        }.distinctUntilChanged(),
        _localNotes
    ) { base, magic, localNotes ->
        CharacterSheetUiData(base = if (localNotes != null) base.copy(notes = localNotes) else base, magic = magic)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val internalControlFlow = combine(_fatalError, _isShopOpen, _interactionError, _pendingActions, _rollResult) { fatal, shop, inter, pending, roll ->
        ControlState(fatal, shop, inter, pending, roll)
    }

    private val lootStateFlow = combine(_lootSearchQuery, _lootSearchResults, merchantManager.state) { query, results, merch ->
        LootState(query, results, merch)
    }

    private val tacticalAndConcentrationFlow = combine(
        _activeTacticalAction,
        _concentrationDialogMessage
    ) { tactical, concentrationDialogMessage ->
        tactical to concentrationDialogMessage
    }

    private val _aggregatedStateFlow = combine(
        uiDataFlow,
        internalControlFlow,
        lootStateFlow,
        tacticalAndConcentrationFlow,
        _showHitDiceDialog
    ) { data, ctrl, loot, tacticalPack, showDialog ->
        AggregatedState(
            data = data,
            control = ctrl,
            loot = loot,
            tactical = tacticalPack.first,
            concentrationDialogMessage = tacticalPack.second,
            showDialog = showDialog
        )
    }

    val state: StateFlow<CharacterSheetUiState> =
        combine(_aggregatedStateFlow, _hitDiceRemainingByType) { aggregated, hitDiceMap ->
            val data = aggregated.data
            val ctrl = aggregated.control
            val loot = aggregated.loot
            val poolViews = data?.base?.hitDicePools?.map { pool ->
                val remaining = hitDiceMap[pool.dieType] ?: pool.count
                HitDicePoolView(pool.dieType, pool.count, remaining)
            } ?: emptyList()

            CharacterSheetUiState(
                isLoading = data == null && ctrl.fatalError == null,
                fatalError = ctrl.fatalError,
                interactionError = ctrl.interactionError ?: ctrl.rollResult,
                isBusy = ctrl.pendingActions.isNotEmpty(),
                isShopOpen = ctrl.isShopOpen,
                merchantState = loot.merchantState,
                pendingActions = ctrl.pendingActions,
                data = data,
                activeTacticalAction = aggregated.tactical,
                lootSearchQuery = loot.query,
                lootSearchResults = loot.results,
                concentrationDialogMessage = aggregated.concentrationDialogMessage,
                showHitDiceDialog = aggregated.showDialog,
                hitDicePoolViews = poolViews
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CharacterSheetUiState())

    init {
        if (characterId == 0L) {
            _fatalError.value = "ID \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0430 \u043d\u0435 \u043f\u0435\u0440\u0435\u0434\u0430\u043d."
        } else {
            merchantManager.init(viewModelScope)
            observeNoteSynchronization()
        }
    }

    init {
        viewModelScope.launch {
            actionHandler.concentrationAlerts.collect { _concentrationDialogMessage.value = it }
        }
    }

    init {
        viewModelScope.launch {
            uiDataFlow.filterNotNull().collect { data ->
                if (_pendingHitDiceReset.value) {
                    resetHitDiceRemaining(data.base.hitDicePools, data.base.remainingHitDice)
                    _pendingHitDiceReset.value = false
                } else {
                    ensureHitDicePoolEntries(data.base.hitDicePools)
                }
            }
        }
    }

    private fun observeNoteSynchronization() {
        viewModelScope.launch {
            repository.getCharacterForSheet(characterId)
                .filterNotNull()
                .collect { char ->
                    if (char.liveState.notes == _localNotes.value) {
                        _localNotes.value = null
                    }
                }
        }
    }


    fun performRoll(action: CombatAction, upcastLevel: Int? = null, slotPreference: SlotPreference = SlotPreference.AUTO) {
        val deactivateParentAfterUse = {
            val parent = action.parentEffectId
            if (!action.isToggle && !parent.isNullOrBlank()) {
                toggleEffect(parent)
            }
        }
        if (action.effectId == "effect_agonizing_blast" || action.name.contains("\u041c\u0443\u0447\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0439")) {
            android.util.Log.d(
                "SheetVM",
                "Action tap: id=${action.uniqueId} name=${action.name} effectId=${action.effectId} " +
                    "parentEffectId=${action.parentEffectId} isToggle=${action.isToggle} isActive=${action.isActive} " +
                    "isBlocked=${action.isBlocked}"
            )
        }
        if (action.isBlocked && !(action.isToggle && action.isActive && action.effectId != null)) {
            android.util.Log.d(
                "SheetVM",
                "Blocked action: id=${action.uniqueId} name=${action.name} effectId=${action.effectId} parentEffectId=${action.parentEffectId} isToggle=${action.isToggle} isActive=${action.isActive}"
            )
            triggerTransientError("\u042d\u0442\u043e \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 \u0441\u0435\u0439\u0447\u0430\u0441 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e")
            return
        }

        if (action.isToggle && action.effectId != null) {
            toggleEffect(action.effectId)
            return
        }

        viewModelScope.launch {

            if (action.type == ActionType.ITEM && action.damageFormula == "\u2014") {
                if (action.resourceId != null) {
                    onResourceSpend(action.resourceId)
                }
                triggerRollSuccess("\u0410\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u043d\u043e: ${action.name}")
                deactivateParentAfterUse()
                return@launch
            }

            val innateSpellIds = state.value.data?.magic?.innateSpellIds ?: emptySet()
            val isItemSourceSpell = action.isSpell && action.sourceUniqueId?.startsWith("item-") == true && action.resourceId != null
            if (action.isSpell && (action.level ?: 0) > 0) {
                val targetLevel = upcastLevel ?: action.level!!
                val effectiveSpellId = action.spellId ?: action.effectId
                val innateSpellId = effectiveSpellId?.takeIf { it in innateSpellIds }
                val spellContext = effectiveSpellId?.let { SpellCastContext(it, action.name, action.isConcentration) }
                val previousConcentrationId = lastChar?.liveState?.concentrationSpellId
                val result = when {
                    isItemSourceSpell && action.resourceId != null -> {
                        actionHandler.handleCast(characterId, CastAction.SpendCharge(action.resourceId))
                    }
                    innateSpellId != null -> {
                        actionHandler.handleCast(characterId, CastAction.SpendInnateUsage(innateSpellId))
                    }
                    else -> {
                        actionHandler.handleCast(characterId, CastAction.SpendSlot(targetLevel, slotPreference, spellContext))
                    }
                }
                if (result.isFailure) {
                    val errorText = when {
                        isItemSourceSpell -> "Заряды предмета исчерпаны"
                        innateSpellId != null -> "Нет доступных заряда"
                        else -> "Нет доступных ячеек ${targetLevel} уровня"
                    }
                    triggerTransientError(errorText)
                    return@launch
                }
                if (innateSpellId == null && !isItemSourceSpell) {
                    showConcentrationReplacementWarning(previousConcentrationId, spellContext)
                }
            }

            if (action.resourceId?.contains("monk") == true || action.resourceId?.contains("ki") == true) {
                val result = actionHandler.handleCast(characterId, CastAction.SpendCharge(action.resourceId))
                if (result.isFailure) {
                    triggerTransientError("\u0417\u0430\u0440\u044f\u0434\u044b \u0438\u0441\u0447\u0435\u0440\u043f\u0430\u043d\u044b")
                    return@launch
                }
            }

            val ammoRefId = action.ammoType
            if (action.type == ActionType.WEAPON && !ammoRefId.isNullOrBlank()) {
                val spendResult = actionHandler.spendAmmoByRefId(characterId, ammoRefId, 1)
                if (spendResult.isFailure) {
                    triggerTransientError(spendResult.exceptionOrNull()?.message ?: "\u0411\u043e\u0435\u043f\u0440\u0438\u043f\u0430\u0441\u044b \u0437\u0430\u043a\u043e\u043d\u0447\u0438\u043b\u0438\u0441\u044c")
                    return@launch
                }
            }



            val isSaveBased = !action.saveDcInfo.isNullOrBlank()
            val isAttackRoll = !isSaveBased && action.hitBonus.isNotBlank()
            var hitInfo = ""
            var isCrit = false

            if (isAttackRoll) {
                val hitBonusValue = action.hitBonus.replace("+", "").toIntOrNull() ?: 0
                val (nat20, totalHit) = diceRoller.rollD20(hitBonusValue)
                isCrit = nat20 == 20
                val isFumble = nat20 == 1

                val header = when {
                    isCrit -> "\uD83D\uDD25 \u041a\u0420\u0418\u0422\u0418\u0427\u0415\u0421\u041a\u0418\u0419 \u0423\u0414\u0410\u0420! "
                    isFumble -> "\uD83D\uDC80 \u041a\u0420\u0418\u0422\u0418\u0427\u0415\u0421\u041a\u0418\u0419 \u041f\u0420\u041e\u041c\u0410\u0425! "
                    else -> ""
                }
                hitInfo = "${header}\uD83C\uDFB2 $totalHit ($nat20) -> "
            } else if (!action.saveDcInfo.isNullOrBlank()) {
                hitInfo = "${action.saveDcInfo} -> "
            }

            val finalFormula = if (upcastLevel != null && action.damageMap.isNotEmpty()) {
                action.damageMap[upcastLevel] ?: action.damageFormula
            } else {
                action.damageFormula
            }

            if (finalFormula != "\u2014" && finalFormula.isNotBlank()) {
                val dmgResult = diceRoller.rollComplex(finalFormula, isCrit)
                val status = "${action.name}: ${hitInfo}\uD83D\uDCA5 ${dmgResult.total} (${action.damageType})"
                triggerRollSuccess(status)
                deactivateParentAfterUse()
            } else if (hitInfo.isNotEmpty()) {

                triggerRollSuccess("${action.name}: $hitInfo \u0410\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u043d\u043e")
                deactivateParentAfterUse()
            }
        }
    }

    fun requestTacticalAction(action: CombatAction) {
        _activeTacticalAction.value = action
    }

    fun dismissTacticalAction() {
        _activeTacticalAction.value = null
    }

    fun executeTacticalCast(level: Int, isPact: Boolean) {
        val action = _activeTacticalAction.value ?: return
        val preference = if (isPact) SlotPreference.PACT_FIRST else SlotPreference.GLOBAL_FIRST
        dismissTacticalAction()
        performRoll(action, upcastLevel = level, slotPreference = preference)
    }

    private fun triggerRollSuccess(message: String) {
        _rollResult.value = message
        viewModelScope.launch {
            delay(5000)
            if (_rollResult.value == message) _rollResult.value = null
        }
    }

    private fun toggleEffect(effectId: String) = execute("toggle_$effectId") { actionHandler.toggleEffect(characterId, effectId) }
    fun onResourceSpend(poolId: String) = execute("charge_$poolId") { actionHandler.handleCast(characterId, CastAction.SpendCharge(poolId)) }
    fun onDeathSaveClick(isSuccess: Boolean) = execute("death_save") { actionHandler.updateDeathSaves(characterId, isSuccess) }
    fun toggleEquipped(id: String) = execute("equip_$id") { actionHandler.toggleEquip(characterId, id) }
    fun toggleAttunement(id: String) = execute("attune_$id") { actionHandler.toggleAttunement(characterId, id) }
    fun updateItemQuantity(id: String, newQty: Int) = execute("qty_$id") {
        val base = uiDataFlow.value?.base ?: return@execute Result.failure(Exception("\u0414\u0430\u043d\u043d\u044b\u0435 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b"))
        val current = (base.weapons + base.armorAndShields + base.gear).find { it.uniqueId == id }?.quantity ?: 0
        actionHandler.updateQuantity(characterId, id, newQty - current)
    }
    fun sellItems(id: String, quantity: Int) = execute("sell_$id") { actionHandler.sellItem(characterId, id, quantity) }
    fun purchaseItem(item: ShopItem) = execute("buy_${item.index}") { actionHandler.purchaseItem(characterId, item) }
    fun toggleShop(open: Boolean) { _isShopOpen.value = open; if (open) merchantManager.loadRoot() }
    fun searchLoot(query: String) {
        _lootSearchQuery.value = query
        lootSearchJob?.cancel()
        if (query.isBlank()) {
            _lootSearchResults.value = emptyList()
            return
        }
        lootSearchJob = viewModelScope.launch {
            delay(350)
            _lootSearchResults.value = libraryRepository.searchAllItems(query)
        }
    }
    fun addLootItem(item: ShopItem) = execute("loot_${item.index}") {
        actionHandler.addLootItem(characterId, item.index).onSuccess {
            _lootSearchQuery.value = ""
            _lootSearchResults.value = emptyList()
        }
    }
    fun processDamage(amount: Int) = execute("hp") { actionHandler.updateHp(characterId, -amount, false) }
    fun processHeal(amount: Int) = execute("hp") { actionHandler.updateHp(characterId, amount, false) }
    fun setTempHp(amount: Int) = execute("hp_temp") { actionHandler.updateHp(characterId, amount, true) }
    fun updateMoney(type: String, delta: Int) = execute("money") { actionHandler.updateMoney(characterId, type, delta) }
    fun performLongRest() = execute("rest") { actionHandler.performRest(characterId, true) }
    fun performDawnReset() = execute("dawn") { actionHandler.performDawnReset(characterId) }
    fun addCondition(condition: String) = execute("add_cond") { actionHandler.addCondition(characterId, condition) }
    fun removeCondition(condition: String) = execute("rem_cond") { actionHandler.removeCondition(characterId, condition) }
    fun increaseExhaustion() = execute("exh_inc") { actionHandler.increaseExhaustion(characterId) }
    fun decreaseExhaustion() = execute("exh_dec") { actionHandler.decreaseExhaustion(characterId) }
    fun resetConcentration() = execute("reset_conc") { actionHandler.resetConcentration(characterId) }
    fun performShortRest() {
        _pendingHitDiceReset.value = true
        _showHitDiceDialog.value = false
        executeWithResult("short_rest", { actionHandler.performRest(characterId, false) }) {
            triggerRollSuccess("\u041a\u043e\u0440\u043e\u0442\u043a\u0438\u0439 \u043e\u0442\u0434\u044b\u0445: \u0440\u0435\u0441\u0443\u0440\u0441\u044b \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u044b")
            _showHitDiceDialog.value = true
        }
    }
    fun spendHitDie(dieType: Int) = executeWithResult("spend_hit_die_$dieType", { actionHandler.spendHitDie(characterId, dieType) }) { healed ->
        decrementHitDicePool(dieType)
        triggerRollSuccess("\u041f\u043e\u0442\u0440\u0430\u0447\u0435\u043d\u0430 \u043a\u043e\u0441\u0442\u044c d$dieType: \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e $healed HP")
    }
    fun dismissHitDiceDialog() { _showHitDiceDialog.value = false }
    fun dismissConcentrationDialog() { _concentrationDialogMessage.value = null }
    fun openPreparation(sourceId: String) {
        viewModelScope.launch {
            val char = lastChar ?: return@launch
            _prepDraft.value = MagicPreparationDraft(
                sourceId = sourceId,
                selectedIds = char.liveState.preparedSpellIds[sourceId] ?: emptySet()
            )
        }
    }
    fun toggleSpellInDraft(spellId: String) = _prepDraft.update { it?.copy(selectedIds = if (spellId in it.selectedIds) it.selectedIds - spellId else it.selectedIds + spellId) }
    fun learnWizardSpell(spellId: String) = executeWithResult("learn_spell_$spellId", { learnWizardSpellUseCase(characterId, spellId) }) {
        triggerRollSuccess("Spell learned")
    }
    fun confirmPreparation() = execute("prep_confirm") {
        val draft = _prepDraft.value ?: return@execute Result.success(Unit)
        actionHandler.syncPreparation(characterId, draft.sourceId, draft.selectedIds).onSuccess { _prepDraft.value = null }
    }
    fun cancelPreparation() { _prepDraft.value = null }
    fun onCast(spell: SpellUiModel) {
        val action = spell.castAction ?: return
        val previousConcentrationId = lastChar?.liveState?.concentrationSpellId
        executeWithResult("cast_${spell.sourceId}_${spell.id}", { actionHandler.handleCast(characterId, action) }) {
            val spellContext = (action as? CastAction.SpendSlot)?.spellContext
            showConcentrationReplacementWarning(previousConcentrationId, spellContext)
        }
    }
    fun onSpendSlotManual(level: Int, isPact: Boolean) {
        val key = if (isPact) "pact_slot_${System.nanoTime()}" else "class_slot_lvl_${level}_${System.nanoTime()}"
        val preference = if (isPact) SlotPreference.PACT_FIRST else SlotPreference.GLOBAL_FIRST
        execute(key) { actionHandler.handleCast(characterId, CastAction.SpendSlot(level, preference)) }
    }

    fun onSpendInnateSlot(slotId: String, spellId: String) = execute(slotId) {
        actionHandler.handleCast(characterId, CastAction.SpendInnateUsage(spellId))
    }

    fun resetWildShape() = execute("wild_shape_reset") {
        repository.syncLiveState(characterId) { live ->
            live.copy(
                transformationId = null,
                transformationHp = 0,
                systemLogs = (live.systemLogs + "\u0414\u0438\u043a\u0438\u0439 \u043e\u0431\u043b\u0438\u043a \u0441\u0431\u0440\u043e\u0448\u0435\u043d").takeLast(10)
            )
        }
    }

    fun updateNotes(text: String) {
        _localNotes.value = text
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            delay(500)
            actionHandler.updateNotes(characterId, text)
        }
    }

    private fun execute(key: String, block: suspend () -> Result<Unit>) {
        executeWithResult(key, block) {}
    }

    private fun <T> executeWithResult(key: String, block: suspend () -> Result<T>, onSuccess: (T) -> Unit) {
        if (_pendingActions.value.contains(key)) return
        _pendingActions.update { it + key }
        viewModelScope.launch {
            try {
                withTimeout(5000L) {
                    val result = block()
                    result.onSuccess { onSuccess(it) }
                    result.onFailure { triggerTransientError(it.message ?: "\u041e\u0448\u0438\u0431\u043a\u0430") }
                }
            } catch (e: Exception) {
                triggerTransientError("\u041e\u0448\u0438\u0431\u043a\u0430: ${e.message}")
            } finally {
                _pendingActions.update { it - key }
            }
        }
    }

    private fun resetHitDiceRemaining(pools: List<HitDicePool>, remaining: Int) {
        val map = pools.associate { it.dieType to it.count }.toMutableMap()
        val target = remaining.coerceAtMost(map.values.sum())
        trimHitDiceMapToTarget(map, target)
        _hitDiceRemainingByType.value = map
    }

    private fun ensureHitDicePoolEntries(pools: List<HitDicePool>) {
        val current = _hitDiceRemainingByType.value.toMutableMap()
        var changed = false
        pools.forEach { pool ->
            val existing = current[pool.dieType]
            if (existing == null) {
                current[pool.dieType] = pool.count
                changed = true
            } else if (existing > pool.count) {
                current[pool.dieType] = pool.count
                changed = true
            }
        }
        if (changed) _hitDiceRemainingByType.value = current
    }

    private fun trimHitDiceMapToTarget(map: MutableMap<Int, Int>, target: Int) {
        var sum = map.values.sum()
        if (sum <= target) return
        val keys = map.keys.sorted()
        var index = 0
        while (sum > target && keys.isNotEmpty()) {
            val key = keys[index % keys.size]
            val value = map[key] ?: 0
            if (value > 0) {
                map[key] = value - 1
                sum--
            }
            index++
        }
    }

    private fun decrementHitDicePool(dieType: Int) {
        _hitDiceRemainingByType.update { current ->
            val currentValue = current[dieType] ?: return@update current
            if (currentValue <= 0) return@update current
            current.toMutableMap().apply { put(dieType, currentValue - 1) }
        }
    }

    private fun triggerTransientError(message: String) {
        errorJob?.cancel()
        _interactionError.value = message
        errorJob = viewModelScope.launch {
            delay(3000)
            _interactionError.value = null
        }
    }

    private fun showConcentrationReplacementWarning(previousSpellId: String?, context: SpellCastContext?) {
        if (context?.isConcentration != true || previousSpellId.isNullOrBlank() || previousSpellId == context.id) return
        val previousName = findSpellNameById(previousSpellId)
        _concentrationDialogMessage.value = "Новая концентрация (${context.name}) сбросила предыдущую: $previousName"
    }

    private fun findSpellNameById(spellId: String): String {
        val fromUi = state.value.data?.magic?.sources
            ?.asSequence()
            ?.flatMap { it.groups.asSequence() }
            ?.flatMap { it.spells.asSequence() }
            ?.firstOrNull { it.id == spellId }
            ?.name
        if (!fromUi.isNullOrBlank()) return fromUi

        val fromSnapshot = lastChar?.snapshot?.magic?.sources
            ?.asSequence()
            ?.flatMap { it.spells.asSequence() }
            ?.firstOrNull { it.id == spellId }
            ?.name
        return fromSnapshot ?: spellId
    }

    private data class ControlState(val fatalError: String?, val isShopOpen: Boolean, val interactionError: String?, val pendingActions: Set<String>, val rollResult: String?)
    private data class LootState(val query: String, val results: List<ShopItem>, val merchantState: MerchantUiState)
    private data class AggregatedState(
        val data: CharacterSheetUiData?,
        val control: ControlState,
        val loot: LootState,
        val tactical: CombatAction?,
        val concentrationDialogMessage: String?,
        val showDialog: Boolean
    )

}


