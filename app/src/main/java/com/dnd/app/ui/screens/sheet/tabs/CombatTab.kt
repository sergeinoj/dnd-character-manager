package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.domain.model.snapshot.ActionType
import com.dnd.app.domain.model.snapshot.CombatAction
import com.dnd.app.domain.model.snapshot.ResourcePoolSnapshot
import com.dnd.app.ui.screens.sheet.CharacterSheetUiState
import com.dnd.app.ui.screens.sheet.CharacterSheetViewModel
import com.dnd.app.ui.screens.sheet.components.*
import com.dnd.app.ui.screens.sheet.components.combat.ClassCombatConsole
import com.dnd.app.ui.screens.sheet.components.combat.CombatActionRow
import com.dnd.app.ui.screens.sheet.components.combat.ConditionBar
import com.dnd.app.ui.screens.sheet.components.common.StatSquare
import com.dnd.app.ui.screens.sheet.components.dialogs.ConditionDialog
import com.dnd.app.ui.screens.sheet.components.health.DeathSavesInteractiveRow
import com.dnd.app.ui.screens.sheet.components.health.HealthWidget
import com.dnd.app.ui.screens.sheet.magic.GlobalSlotsUiModel
import com.dnd.app.util.DndLocalization


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatTab(
    state: CharacterSheetUiState,
    viewModel: CharacterSheetViewModel,
    onTransform: () -> Unit
) {
    val data = state.data?.base ?: return
    val magic = state.data.magic
    val isUnconscious = data.hpCurrent <= 0
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    var familiarSheet by remember { mutableStateOf<MonsterRecord?>(null) }
    var familiarAction by remember { mutableStateOf<CombatAction?>(null) }
    val familiarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConditionDialog by remember { mutableStateOf(false) }

    if (showConditionDialog) {
        ConditionDialog(
            activeConditions = data.activeConditions,
            exhaustionLevel = data.exhaustionLevel,
            onAdd = { viewModel.addCondition(it) },
            onRemove = { viewModel.removeCondition(it) },
            onIncreaseExhaustion = { viewModel.increaseExhaustion() },
            onDecreaseExhaustion = { viewModel.decreaseExhaustion() },
            onDismiss = { showConditionDialog = false }
        )
    }

    if (familiarSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { familiarSheet = null; familiarAction = null },
            sheetState = familiarSheetState
        ) {
            FamiliarCard(
                familiar = familiarSheet!!,
                action = familiarAction,
                onClose = { familiarSheet = null; familiarAction = null }
            )
        }
    }

    fun isFamiliarAction(action: CombatAction): Boolean {
        if (data.familiar == null) return false
        val idHit = action.uniqueId.contains("familiar", ignoreCase = true) ||
            (action.effectId?.contains("familiar", ignoreCase = true) == true)
        val nameHit = action.name.contains("Фамильяр", ignoreCase = true) ||
            action.name.contains("familiar", ignoreCase = true)
        return idHit || nameHit
    }

    val onActionTap: (CombatAction) -> Unit = { action ->
        viewModel.performRoll(action)
    }

    val onActionLongTap: (CombatAction) -> Unit = { action ->
        when {
            isFamiliarAction(action) -> {
                familiarSheet = data.familiar
                familiarAction = action
            }
            action.isSpell && (action.level ?: 0) > 0 -> {
                if (magic.globalSlots?.isVisible == true) {
                    viewModel.requestTacticalAction(action)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (data.isTransformed) Color(0xFFFFF8E1) else Color.Transparent)
        ) {


            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp).zIndex(2f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthWidget(
                    current = data.hpCurrent, max = data.hpMax, temp = data.hpTemp,
                    onDamage = viewModel::processDamage,
                    onHeal = viewModel::processHeal,
                    onSetTempHp = viewModel::setTempHp,
                    isTransformed = data.isTransformed,
                    transformationHp = data.transformationHp,
                    transformationMaxHp = data.transformedMonster?.hitPoints ?: data.transformationHp
                )

                if (isUnconscious) {
                    DeathSavesInteractiveRow(state = data.deathSaves, onSaveClick = viewModel::onDeathSaveClick)
                }

                if (!isUnconscious) {
                    val consoleActions = data.filteredCombatActions.filter { action ->
                        action.type == ActionType.FEATURE_TOGGLE ||
                            action.resourceId != null ||
                            (action.type == ActionType.ITEM && action.sourceUniqueId == null)
                    }
                    val actionBasedPools = consoleActions
                        .mapNotNull { it.resourceId }
                        .distinct()
                        .map { poolId ->
                            val max = consoleActions
                                .asSequence()
                                .filter { it.resourceId == poolId }
                                .mapNotNull { it.maxCharges }
                                .maxOrNull()
                                ?: 1
                            ResourcePoolSnapshot(
                                id = poolId,
                                name = DndLocalization.translateProficiency(poolId),
                                max = max
                            )
                        }
                    val consolePools = data.classResources + actionBasedPools.filter { actionPool ->
                        data.classResources.none { it.id == actionPool.id }
                    }
                    ConditionBar(
                        activeConditions = data.activeConditions,
                        exhaustionLevel = data.exhaustionLevel,
                        isConcentrating = data.isConcentrating,
                        onAddClick = { showConditionDialog = true },
                        onRemoveCondition = { viewModel.removeCondition(it) },
                        onDecreaseExhaustion = { viewModel.decreaseExhaustion() },
                        onResetConcentration = { viewModel.resetConcentration() }
                    )
                    ClassCombatConsole(
                        resourcePools = consolePools,
                        resourceCharges = data.resourceCharges,
                        classActions = consoleActions,
                        activeEffects = data.activeEffects,
                        onSpendResource = viewModel::onResourceSpend,
                        onActionClick = { viewModel.performRoll(it) },
                        onTransform = onTransform,
                        isTransformed = data.isTransformed,
                        transformationName = data.transformationName,
                        onResetTransformation = viewModel::resetWildShape,
                        maxHeightDp = screenHeightDp / 2
                    )
                }
            }


            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (!isUnconscious) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatSquare(Modifier.weight(1f).fillMaxHeight(), "КД", data.displayArmorClass.toString())
                                StatSquare(Modifier.weight(1f).fillMaxHeight(), "Инициатива", data.initiative)
                                StatSquare(Modifier.weight(1f).fillMaxHeight(), "Скорость", data.displaySpeed)
                            }
                        }
                    }

                    val pactResourceName = DndLocalization.translateProficiency("Pact Slots")
                    val hasPactResourceInConsole = data.classResources.any {
                        it.name.equals(pactResourceName, ignoreCase = true)
                    }
                    val combatSlotsModel = magic.globalSlots?.takeIf { it.isVisible }?.let { base ->
                        if (hasPactResourceInConsole && base.pactSlots != null && base.classSlots.isEmpty()) {
                            null
                        } else {
                            GlobalSlotsUiModel(
                                classSlots = base.classSlots,
                                innateSlots = base.innateSlots,
                                pactSlots = if (hasPactResourceInConsole) null else base.pactSlots,
                                showClassSlots = base.showClassSlots
                            )
                        }
                    }
                    combatSlotsModel?.takeIf { it.isVisible }?.let { slotsModel ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF424242), MaterialTheme.shapes.small)
                                    .padding(8.dp)
                            ) {
                                GlobalSlotsSection(
                                    slotsModel,
                                    viewModel::onSpendSlotManual,
                                )
                            }
                        }
                    }

                    val weapons = data.filteredCombatActions.filter { it.type == ActionType.WEAPON }
                    val cantrips = data.filteredCombatActions.filter { it.type == ActionType.CANTRIP }
                    val spells = data.filteredCombatActions.filter { it.type == ActionType.SPELL }
                    val itemsAct = data.filteredCombatActions.filter {
                        it.type == ActionType.ITEM && it.resourceId == null && it.sourceUniqueId != null
                    }
                    val monsterActions = if (data.isTransformed) {
                        data.transformedMonster?.actions.orEmpty().mapIndexed { idx, action ->
                            val toHit = action.attackBonus?.let { if (it >= 0) "+$it" else it.toString() } ?: ""
                            val dmg = action.damage.joinToString(" + ") { part ->
                                part.dice
                            }.ifBlank { "—" }
                            val translatedTypes = action.damage
                                .mapNotNull { it.type?.takeIf { t -> t.isNotBlank() } }
                                .map(DndLocalization::translateDamageType)
                                .distinct()
                                .joinToString(" / ")
                            CombatAction(
                                uniqueId = "monster_${data.transformedMonster?.index ?: "shape"}_${idx}_${action.name}",
                                name = action.name,
                                hitBonus = toHit,
                                damageFormula = dmg,
                                damageType = translatedTypes,
                                range = action.range ?: "5 фт.",
                                type = ActionType.WEAPON,
                                description = action.description
                            )
                        }
                    } else {
                        emptyList()
                    }

                    if (monsterActions.isNotEmpty()) {
                        item { CombatHeader("ДЕЙСТВИЯ ЗВЕРЯ") }
                        items(monsterActions, key = { "beast_${it.name}_${it.range}" }) { action ->
                            CombatActionRow(
                                action = action,
                                onClick = { onActionTap(action) },
                                onLongClick = {},
                                enableLongClick = false
                            )
                        }
                    }

                    if (!data.isTransformed && weapons.isNotEmpty()) {
                        item { CombatHeader("ОРУЖИЕ") }
                        items(weapons, key = { "weap_${it.uniqueId}" }) { action ->
                            CombatActionRow(
                                action = action,
                                onClick = { onActionTap(action) },
                                onLongClick = { onActionLongTap(action) },
                                enableLongClick = isFamiliarAction(action)
                            )
                        }
                    }

                    if (cantrips.isNotEmpty()) {
                        item { CombatHeader("ЗАГОВОРЫ") }
                        items(cantrips, key = { "cant_${it.uniqueId}" }) { action ->
                            CombatActionRow(
                                action = action,
                                onClick = { onActionTap(action) },
                                onLongClick = { onActionLongTap(action) },
                                enableLongClick = isFamiliarAction(action)
                            )
                        }
                    }

                    if (spells.isNotEmpty()) {
                        item { CombatHeader("ЗАКЛИНАНИЯ") }
                        items(spells, key = { "spell_${it.uniqueId}" }) { action ->
                            CombatActionRow(
                                action = action,
                                onClick = { onActionTap(action) },
                                onLongClick = { onActionLongTap(action) },
                                enableLongClick = true,
                                isConcentratingThisSpell = action.spellId == data.concentrationSpellId
                            )
                        }
                    }

                    if (itemsAct.isNotEmpty()) {
                        item { CombatHeader("ТЕХНИКИ И АРТЕФАКТЫ") }
                        items(itemsAct, key = { "item_${it.uniqueId}" }) { action ->
                            CombatActionRow(
                                action = action,
                                onClick = { onActionTap(action) },
                                onLongClick = { onActionLongTap(action) },
                                enableLongClick = isFamiliarAction(action)
                            )
                        }
                    }

                    item { CombatHeader("ИНВЕНТАРЬ (БЫСТРАЯ СМЕНА)") }
                    items(data.weapons + data.armorAndShields, key = { "swap_${it.uniqueId}" }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, Color(0xFF9E9E9E)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Checkbox(
                                checked = item.isEquipped,
                                onCheckedChange = { viewModel.toggleEquipped(item.uniqueId) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                enabled = !state.isBusy
                            )
                        }
                    }
                }

                if (isUnconscious) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).zIndex(1f).clickable(enabled = false) {})
                }
            }
        }
    }
}

@Composable
private fun FamiliarCard(
    familiar: MonsterRecord,
    action: CombatAction?,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.Transparent
    val speedLabel = familiar.speed.entries.joinToString(" / ") { "${DndLocalization.translateSpeed(it.key)}: ${it.value}" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(familiar.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
                Text(
                    listOfNotNull(
                        familiar.size?.let(DndLocalization::translateMonsterSize),
                        familiar.type?.let(DndLocalization::translateMonsterType),
                        familiar.alignment?.let(DndLocalization::translateAlignment)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatSquare(modifier = Modifier.weight(1f), title = "БА", value = familiar.armorClass?.toString() ?: "—")
            StatSquare(modifier = Modifier.weight(1f), title = "ХП", value = familiar.hitPoints?.toString() ?: "—")
            StatSquare(
                modifier = Modifier.weight(1f),
                title = "Скорость",
                value = speedLabel.ifBlank { "—" },
                valueTextSize = 10.sp
            )
        }

        val stats = familiar.stats
        if (stats.isNotEmpty()) {
            val order = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                order.forEach { code ->
                    val valStr = stats[code]?.toString() ?: "—"
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(DndLocalization.translateStat(code), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(valStr, fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColor)
                    }
                }
            }
        }

        action?.let { act ->
            Column(
                modifier = Modifier.fillMaxWidth().background(cardBg, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Быстрая атака", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
                Text(act.name, fontWeight = FontWeight.Medium, color = textColor)
                val damage = act.damageFormula.ifBlank { "—" }
                val toHit = act.hitBonus.ifBlank { "—" }
                val range = act.range
                Text("Бонус атаки: $toHit", color = textColor)
                Text("Урон: $damage", color = textColor)
                Text("Дистанция: $range", color = textColor)
                act.saveDcInfo?.let { Text(it, color = textColor) }
            }
        }

        if (familiar.actions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Действия", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
                familiar.actions.forEach { act ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(cardBg, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(8.dp)
                    ) {
                        Text(act.name, fontWeight = FontWeight.Medium, color = textColor)
                        val toHit = act.attackBonus?.let { if (it >= 0) "+$it" else "$it" } ?: "—"
                        val dmg = act.damage.joinToString(" + ") { dmg -> listOfNotNull(dmg.dice, dmg.type).joinToString(" ") }.ifBlank { "—" }
                        val range = act.range ?: "—"
                        Text("Бонус атаки: $toHit", fontSize = 12.sp, color = textColor)
                        Text("Урон: $dmg", fontSize = 12.sp, color = textColor)
                        Text("Дистанция: $range", fontSize = 12.sp, color = textColor)
                        act.description?.let { Text(it, fontSize = 12.sp, color = textColor) }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
        ) { Text("Закрыть", color = textColor) }
    }
}

@Composable
private fun CombatHeader(title: String) {
    Column {
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Divider(color = Color.White.copy(alpha = 0.9f), thickness = 0.5.dp)
    }
}


