// --- НАЧАЛО ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\StatsTab.kt
package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.snapshot.StatModel
import com.dnd.app.ui.screens.sheet.CharacterSheetUiState
import com.dnd.app.ui.screens.sheet.components.*
import com.dnd.app.ui.screens.sheet.components.DefenseLayerSheet
import com.dnd.app.ui.screens.sheet.components.SensesLayerSheet
import com.dnd.app.ui.screens.sheet.components.dialogs.HpModifierDialog
import com.dnd.app.ui.screens.sheet.components.dialogs.TempHpDialog
import com.dnd.app.ui.screens.sheet.components.money.MoneyWidget
import com.dnd.app.ui.theme.DndBonusGreen
import com.dnd.app.ui.theme.DndMalusRed

private object LayoutSettings {
    const val WEIGHT_HEADER = 0.18f
    const val WEIGHT_COMBAT = 0.10f
    const val WEIGHT_HP = 0.22f
    const val WEIGHT_SEC_STATS = 0.14f
    const val WEIGHT_MONEY = 0.16f
    const val WEIGHT_REST = 0.09f
    val FONT_STAT_NAME = 10.sp
    val FONT_STAT_VALUE = 32.sp
    val GAP_DEFAULT = 4.dp
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun StatsTab(
    state: CharacterSheetUiState,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onSetTempHp: (Int) -> Unit,
    onMoneyUpdate: (String, Int) -> Unit,
    onLongRest: () -> Unit,
    onShortRest: () -> Unit,
    onDawnReset: () -> Unit,
    onSpendHitDie: (Int) -> Unit
) {
    val data = state.data?.base ?: return
    var showDefenseLayer by remember { mutableStateOf(false) }
    var showSensesLayer by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(LayoutSettings.GAP_DEFAULT)) {
        val totalHeight = maxHeight
        val statSize = (totalHeight - 24.dp) / 6
        val statsMap = data.stats.associateBy { it.code }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (data.isTransformed) Color(0xFFFFF8E1) else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.width(statSize).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                val itemMod = Modifier.height(statSize).fillMaxWidth()
                statsMap["STR"]?.let { StatBoxStrict(itemMod, "\u0421\u0438\u043B\u0430", it, highlightWildShape = data.isTransformed) }
                statsMap["DEX"]?.let { StatBoxStrict(itemMod, "\u041B\u043E\u0432\u043A\u043E\u0441\u0442\u044C", it, highlightWildShape = data.isTransformed) }
                statsMap["CON"]?.let { StatBoxStrict(itemMod, "\u0422\u0435\u043B\u043E\u0441.", it, highlightWildShape = data.isTransformed) }
                statsMap["INT"]?.let { StatBoxStrict(itemMod, "\u0418\u043D\u0442\u0435\u043B\u043B\u0435\u043A\u0442", it) }
                statsMap["WIS"]?.let { StatBoxStrict(itemMod, "\u041C\u0443\u0434\u0440\u043E\u0441\u0442\u044C", it) }
                statsMap["CHA"]?.let { StatBoxStrict(itemMod, "\u0425\u0430\u0440\u0438\u0437\u043C\u0430", it) }
            }

            Spacer(modifier = Modifier.width(LayoutSettings.GAP_DEFAULT))

                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    DefenseLayerSheet(
                        visible = showDefenseLayer,
                        isTransformed = data.isTransformed,
                        heroResistances = data.heroDefenseResistances,
                        heroImmunities = data.heroDefenseImmunities,
                        beastResistances = data.beastDefenseResistances,
                        beastImmunities = data.beastDefenseImmunities,
                        onDismiss = { showDefenseLayer = false }
                    )
                    SensesLayerSheet(
                        visible = showSensesLayer,
                        senses = data.senses,
                        languages = data.languages,
                        onDismiss = { showSensesLayer = false }
                    )
                Column(modifier = Modifier.weight(LayoutSettings.WEIGHT_HEADER), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                        Text(data.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(6.dp), contentAlignment = Alignment.CenterStart) {
                            Text(data.classTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Box(modifier = Modifier.width(statSize).fillMaxHeight().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(data.level.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Row(modifier = Modifier.weight(LayoutSettings.WEIGHT_COMBAT).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SquareInfoBox(
                        "\u041A\u0414",
                        data.displayArmorClass.toString(),
                        Modifier.weight(1f).fillMaxHeight(),
                        showDetailsHint = true,
                        onClick = { showDefenseLayer = true }
                    )
                    SquareInfoBox("\u0411\u041C", data.proficiencyBonus, Modifier.weight(1f).fillMaxHeight(), isBonus = true)
                }

                HealthControlWidget(
                    current = data.hpCurrent,
                    temp = data.hpTemp,
                    max = data.hpMax,
                    isTransformed = data.isTransformed,
                    transformationHp = data.transformationHp,
                    transformationMaxHp = data.transformedMonster?.hitPoints ?: data.transformationHp,
                    onDamage = onDamage,
                    onHeal = onHeal,
                    onSetTempHp = onSetTempHp,
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_HP)
                )
                val beastPassivePerception = data.transformedMonster?.senses
                    ?.entries
                    ?.firstOrNull { it.key.equals("passive_perception", ignoreCase = true) }
                    ?.value
                val beastWalkSpeed = data.transformedMonster?.speed?.get("walk")
                    ?: data.transformedMonster?.speed?.entries?.firstOrNull()?.value
                val sensesValue = if (data.isTransformed && !beastPassivePerception.isNullOrBlank()) {
                    beastPassivePerception
                } else {
                    data.passivePerception
                }
                val speedValue = if (data.isTransformed && !beastWalkSpeed.isNullOrBlank()) {
                    beastWalkSpeed
                } else {
                    data.displaySpeed
                }
                Row(modifier = Modifier.weight(LayoutSettings.WEIGHT_SEC_STATS).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactInfoBox("\u0418\u043D\u0438\u0446\u0438\u0430\u0442\u0438\u0432\u0430", data.initiative, Modifier.weight(1f).fillMaxHeight())
                    CompactInfoBox(
                        title = "\u041F\u0430\u0441. \u0432\u043D\u0438\u043C.",
                        value = sensesValue,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        showDetailsHint = true,
                        onClick = { showSensesLayer = true }
                    )
                    CompactInfoBox("\u0421\u043A\u043E\u0440\u043E\u0441\u0442\u044C", speedValue, Modifier.weight(1f).fillMaxHeight())
                }

                MoneyWidget(coins = data.coins, onUpdate = onMoneyUpdate, modifier = Modifier.weight(LayoutSettings.WEIGHT_MONEY))

                Row(modifier = Modifier.weight(LayoutSettings.WEIGHT_REST).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RestButton("\u041A\u043E\u0440\u043E\u0442\u043A\u0438\u0439", Modifier.weight(1f).fillMaxHeight(), onClick = onShortRest)
                    RestButton("\u2600\uFE0F", Modifier.weight(0.6f).fillMaxHeight(), onClick = onDawnReset)
                    RestButton("\u0414\u043B\u0438\u043D\u043D\u044B\u0439", Modifier.weight(1f).fillMaxHeight(), onClick = onLongRest)
                }
            }
        }
    }
}

@Composable
fun StatBoxStrict(modifier: Modifier, name: String, stat: StatModel, highlightWildShape: Boolean = false) {
    val mod = stat.modifier.toIntOrNull() ?: 0
    val bg = if (highlightWildShape) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surface
    val border = if (highlightWildShape) Color(0xFFFDE68A) else Color.Gray
    Column(modifier = modifier.background(bg, RoundedCornerShape(4.dp)).border(1.dp, border, RoundedCornerShape(4.dp)).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text(name, fontSize = LayoutSettings.FONT_STAT_NAME, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(text = stat.modifier, fontSize = LayoutSettings.FONT_STAT_VALUE, fontWeight = FontWeight.Black, color = if (mod > 0) DndBonusGreen else if (mod < 0) DndMalusRed else MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp)) {
            Text(stat.value.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HealthControlWidget(
    current: Int,
    temp: Int,
    max: Int,
    isTransformed: Boolean = false,
    transformationHp: Int = 0,
    transformationMaxHp: Int = 0,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onSetTempHp: (Int) -> Unit,
    modifier: Modifier
) {
    val shownCurrent = if (isTransformed) transformationHp else current
    val shownMax = if (isTransformed) transformationMaxHp.coerceAtLeast(1) else max.coerceAtLeast(1)
    var showHpDialog by remember { mutableStateOf(false) }
    var showTempHpDialog by remember { mutableStateOf(false) }

    if (showHpDialog) {
        HpModifierDialog(
            title = "\u0423\u0440\u043E\u043D / \u041B\u0435\u0447\u0435\u043D\u0438\u0435",
            onDismiss = { showHpDialog = false },
            onConfirmDamage = { onDamage(it); showHpDialog = false },
            onConfirmHeal = { onHeal(it); showHpDialog = false }
        )
    }

    if (showTempHpDialog) {
        TempHpDialog(
            title = "\u0412\u0440\u0435\u043C\u0435\u043D\u043D\u044B\u0435 \u0445\u0438\u0442\u044B",
            onDismiss = { showTempHpDialog = false },
            onConfirm = { amount: Int ->
                onSetTempHp(amount)
                showTempHpDialog = false
            }
        )
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HealthButton("-1", DndMalusRed, Modifier.weight(0.2f).fillMaxHeight()) { onDamage(1) }

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .clickable { showHpDialog = true },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (isTransformed) "\u0424\u043E\u0440\u043C\u0430 \u0437\u0432\u0435\u0440\u044F" else "\u0417\u0434\u043E\u0440\u043E\u0432\u044C\u0435", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$shownCurrent/$shownMax", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (isTransformed) {
                Text("\u0422\u0435\u043B\u043E \u0433\u0435\u0440\u043E\u044F: $current/$max", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(top = 2.dp)
                    .clickable { showTempHpDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (temp > 0) {
                    Text("+$temp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
                } else {
                    Text("0 Temp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }

        HealthButton("+1", DndBonusGreen, Modifier.weight(0.2f).fillMaxHeight()) { onHeal(1) }
    }
}

@Composable
fun RestButton(text: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable(enabled = onClick != null, onClick = onClick ?: {}), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CompactInfoBox(
    title: String,
    value: String,
    modifier: Modifier,
    showDetailsHint: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 9.sp, lineHeight = 10.sp, textAlign = TextAlign.Center, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        if (showDetailsHint) {
            Text(
                text = "⋮",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 1f)
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\StatsTab.kt





