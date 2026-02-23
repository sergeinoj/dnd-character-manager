package com.dnd.app.ui.screens.sheet

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.sheet.magic.CastAction
import com.dnd.app.ui.screens.sheet.magic.MagicPreparationDraft
import com.dnd.app.ui.screens.sheet.magic.TacticalCastDialog
import com.dnd.app.ui.screens.sheet.magic.SpellUiModel
import com.dnd.app.ui.screens.sheet.shop.ShopModal
import com.dnd.app.ui.screens.sheet.tabs.*
import com.dnd.app.ui.screens.sheet.components.HitDiceRecoveryDialog
import com.dnd.app.ui.theme.DndBackground
import kotlinx.coroutines.launch

private const val EDIT_MODE_NOTES_KEY = "masters_key"
private const val SHEET_PREFS = "sheet_prefs"
private const val KEY_LAST_TAB_INDEX = "last_tab_index"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterSheetScreen(
    navigateUp: () -> Unit,
    onEditCharacter: () -> Unit,
    onLevelUp: () -> Unit,
    onOpenShapeSelector: () -> Unit,
    viewModel: CharacterSheetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(SHEET_PREFS, Context.MODE_PRIVATE) }
    var selectedTabIndex by remember { mutableIntStateOf(prefs.getInt(KEY_LAST_TAB_INDEX, 3)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val base = state.data?.base
    val editModeUnlocked = base?.notes?.contains(EDIT_MODE_NOTES_KEY, ignoreCase = true) == true
    HitDiceRecoveryDialog(
        visible = state.showHitDiceDialog,
        hitDicePools = state.hitDicePoolViews,
        remainingDice = base?.remainingHitDice ?: 0,
        totalDice = base?.totalHitDice ?: 0,
        hitDiceFormula = base?.hitDiceFormula ?: "",
        onSpendDie = { viewModel.spendHitDie(it) },
        onDismissRequest = viewModel::dismissHitDiceDialog
    )
    val globalSlots = state.data?.magic?.globalSlots
    state.concentrationDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConcentrationDialog,
            title = { Text("\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u043a\u043e\u043d\u0446\u0435\u043d\u0442\u0440\u0430\u0446\u0438\u0438") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissConcentrationDialog) {
                    Text("\u041f\u043e\u043d\u044f\u0442\u043d\u043e")
                }
            }
        )
    }
    state.activeTacticalAction?.takeIf { globalSlots != null }?.let { action ->
        TacticalCastDialog(
            action = action,
            globalSlots = globalSlots,
            onSpendSlot = { level, isPact -> viewModel.executeTacticalCast(level, isPact) },
            onDismissRequest = viewModel::dismissTacticalAction
        )
    }

    LaunchedEffect(state.interactionError) {
        state.interactionError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            }
        }
    }

    if (state.isShopOpen) {
        ShopModal(
            state = state,
            onDismiss = { viewModel.toggleShop(false) },
            merchantManager = viewModel.merchantManager,
            onPurchase = viewModel::purchaseItem
        )
    }

    val tabs = listOf(
        TabItem("\u041c\u0430\u0433\u0438\u044f", Icons.Filled.Star),
        TabItem("\u0411\u043e\u0439", Icons.Filled.Build),
        TabItem("\u0413\u043b\u0430\u0432\u043d\u0430\u044f", Icons.Filled.AccountBox),
        TabItem("\u041d\u0430\u0432\u044b\u043a\u0438", Icons.Filled.List),
        TabItem("\u0421\u043d\u0430\u0440\u044f\u0436.", Icons.Filled.ShoppingCart),
        TabItem("\u0411\u0438\u043e", Icons.Filled.Face),
        TabItem("\u0417\u0430\u043c\u0435\u0442\u043a\u0438", Icons.Filled.Create)
    )
    val safeSelectedTab = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    if (safeSelectedTab != selectedTabIndex) selectedTabIndex = safeSelectedTab
    LaunchedEffect(selectedTabIndex) {
        prefs.edit().putInt(KEY_LAST_TAB_INDEX, selectedTabIndex).apply()
    }

    Scaffold(
        topBar = {
            val isActionEnabled = !state.isLoading && (base?.level ?: 0) < 20
            DndActionTopBar(
                title = base?.name ?: "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...",
                onBack = navigateUp,
                actionIcon = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                enabled = isActionEnabled,
                                onClick = onLevelUp,
                                onLongClick = if (editModeUnlocked) {
                                    {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEditCharacter()
                                    }
                                } else null
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ArrowCircleUp,
                                contentDescription = "Level Up (Click) / Edit (Long Click)",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "Lvl Up",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onActionClick = null,
                isActionEnabled = isActionEnabled
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val tabWidth = maxWidth / 6
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    tabs.forEachIndexed { index, item ->
                        CustomBottomNavItem(
                            modifier = Modifier.width(tabWidth),
                            item = item,
                            isSelected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }
            state.fatalError != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(state.fatalError!!, color = MaterialTheme.colorScheme.error)
                }
            }
            base != null -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTabIndex) {
                        0 -> SpellsTab(
                            state = state,
                            onCast = viewModel::onCast,
                            onSpendSlot = viewModel::onSpendSlotManual,
                            onOpenPreparation = viewModel::openPreparation,
                            onToggleSpell = viewModel::toggleSpellInDraft,
                            onLearnSpell = viewModel::learnWizardSpell,
                            onConfirmPreparation = viewModel::confirmPreparation,
                            onCancelPreparation = viewModel::cancelPreparation
                        )
                        1 -> CombatTab(state = state, viewModel = viewModel, onTransform = onOpenShapeSelector)
                        2 -> StatsTab(
                            state = state,
                            onDamage = viewModel::processDamage,
                            onHeal = viewModel::processHeal,
                            onSetTempHp = viewModel::setTempHp,
                            onMoneyUpdate = viewModel::updateMoney,
                            onLongRest = viewModel::performLongRest,
                            onShortRest = viewModel::performShortRest,
                            onDawnReset = viewModel::performDawnReset,
                            onSpendHitDie = viewModel::spendHitDie
                        )
                        3 -> SkillsTab(
                            stats = base.stats,
                            skillsByStat = base.skillsByStat,
                            proficiencyBonus = base.proficiencyBonus,
                            toolProficiencies = base.toolProficiencies,
                            proficiencies = base.proficiencies,
                            proficiencyLabels = base.proficiencyLabels
                        )
                        4 -> InventoryTab(
                            state = state,
                            viewModel = viewModel,
                            onEquipToggle = viewModel::toggleEquipped,
                            onQuantityChange = viewModel::updateItemQuantity,
                            onMoneyUpdate = viewModel::updateMoney,
                            onSellItem = viewModel::sellItems,
                            onOpenShop = { viewModel.toggleShop(true) }
                        )
                        5 -> BioTab(autoLore = base.autoLore, manualBioFields = base.manualBioFields)
                        6 -> NotesTab(
                            logs = base.systemLogs,
                            notes = base.notes,
                            onUpdate = viewModel::updateNotes
                        )
                    }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun CustomBottomNavItem(
    modifier: Modifier = Modifier,
    item: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color.White else Color.LightGray

    Column(
        modifier = modifier
            .run { if (isSelected) height(60.dp) else height(56.dp) }
            .background(bg)
            .border(1.dp, Color.Gray)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Text(
            text = item.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

