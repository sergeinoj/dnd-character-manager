// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\components\SpellComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.ui.components.shared.spell.SpellDetailsBlock
import com.dnd.app.ui.components.shared.spell.UnifiedSpellListItemStyle
import com.dnd.app.ui.screens.sheet.magic.*
import com.dnd.app.ui.screens.sheet.components.SheetUiConfig
import com.dnd.app.util.DndLocalization

@Composable
fun GlobalSlotsSection(
    model: GlobalSlotsUiModel,
    onSpendSlot: (level: Int, isPact: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SheetUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(SheetUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        if (model.showClassSlots && model.classSlots.isNotEmpty()) {
            SlotsSection(
                title = DndLocalization.translateClassSlotsHeader(),
                slotLevels = model.classSlots,
                onSpend = { onSpendSlot(it.level, false) }
            )
        }
        model.pactSlots?.let { pact ->
            SlotsSection(
                title = DndLocalization.translatePactMagicHeader(),
                slotLevels = listOf(pact),
                onSpend = { onSpendSlot(pact.level, true) },
                labelProvider = { "P" }
            )
        }
    }
}

@Composable
private fun SlotsSection(
    title: String,
    slotLevels: List<SpellSlotLevelUiModel>,
    onSpend: (SpellSlotLevelUiModel) -> Unit,
    isClickable: Boolean = true,
    headerSuffix: String? = null,
    labelProvider: (SpellSlotLevelUiModel) -> String = { it.level.toString() }
) {
    if (slotLevels.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(SheetUiConfig.SECTION_BORDER_WIDTH, SheetUiConfig.SECTION_BORDER_COLOR)
            .background(SheetUiConfig.SECTION_CONTENT_BG_COLOR),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SectionHeader(title, headerSuffix)
        Column(
            modifier = Modifier.padding(SheetUiConfig.SECTION_CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            slotLevels.forEach { levelModel ->
                SlotRow(
                    label = labelProvider(levelModel),
                    slots = levelModel.slots,
                    onSpend = { onSpend(levelModel) },
                    isClickable = isClickable
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, suffix: String?) {
    val headerText = if (suffix.isNullOrBlank()) title else "$title $suffix"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetUiConfig.SECTION_HEADER_BG_COLOR)
            .padding(
                horizontal = SheetUiConfig.SECTION_HEADER_PADDING_HORIZONTAL,
                vertical = SheetUiConfig.SECTION_HEADER_PADDING_VERTICAL
            )
    ) {
        Text(
            text = headerText,
            color = SheetUiConfig.SECTION_HEADER_TEXT_COLOR,
            fontSize = SheetUiConfig.SECTION_HEADER_FONT_SIZE,
            fontWeight = SheetUiConfig.SECTION_HEADER_FONT_WEIGHT
        )
    }
}

@Composable
fun SlotRow(label: String, slots: List<SpellSlotCircleUiModel>, onSpend: () -> Unit, isClickable: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(28.dp).background(Color.DarkGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            slots.forEach { slot ->
                SlotCircle(isSpent = slot.isSpent, isPending = slot.isPending, isClickable = isClickable, onClick = onSpend)
            }
        }
    }
}

@Composable
fun SlotCircle(isSpent: Boolean, isPending: Boolean, isClickable: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, Color.White, CircleShape)
            .background(if (isSpent || isPending) Color.DarkGray else Color.White, CircleShape)
            .clickable(enabled = isClickable && !isSpent && !isPending) { onClick() }
    )
}

@Composable
fun ChargesBar(poolId: String, max: Int, spent: Int, isPending: Boolean, onSpend: (String) -> Unit) {
    if (max <= 0) return
    val displayMax = minOf(max, 20)
    val displaySpent = minOf(spent, 20)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(displayMax) { index ->
            val isChargeSpent = index < displaySpent
            val isThisPending = isPending && index == displaySpent
            Box(
                modifier = Modifier.size(10.dp).border(1.dp, Color.White, CircleShape)
                    .background(if (isChargeSpent || isThisPending) Color.Transparent else Color(0xFF90CAF9), CircleShape)
                    .clickable(enabled = !isChargeSpent && !isPending) { onSpend(poolId) }
            )
        }
        if (max > 20) Text("+${max - 20}", color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun MagicSourceCard(source: SpellSourceUiModel, pendingActions: Set<String>, onPrepare: () -> Unit, onCast: (SpellUiModel) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF424242)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = source.title.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = source.statsInfo, color = Color.LightGray, fontSize = 11.sp)
                }
                if (source.exclusiveResourcePoolId != null && source.maxCharges > 0) {
                    val isPending = pendingActions.contains("charge_${source.exclusiveResourcePoolId}")
                    ChargesBar(poolId = source.exclusiveResourcePoolId, max = source.maxCharges, spent = source.spentCharges, isPending = isPending, onSpend = {  })
                } else if (source.chargesText != null) {
                    Text(source.chargesText, color = Color(0xFF90CAF9), fontSize = 12.sp)
                }
                if (source.canPrepare) {
                    IconButton(onClick = onPrepare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Book, "Prepare", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            source.groups.forEach { group ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(text = group.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    group.spells.forEach { spell ->
                        SpellRow(spell = spell, onCast = onCast)
                        Divider(color = Color(0xFFBDBDBD), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SpellRow(spell: SpellUiModel, onCast: (SpellUiModel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val castIconColor = if (spell.isResourceExhausted) Color.Gray else if (spell.isUpcast) Color(0xFFFF5722) else Color(0xFF7B1FA2)
    val isDarkTheme = isSystemInDarkTheme()
    val rowBg = if (spell.isCurrentConcentration) {
        if (isDarkTheme) Color(0xFF4A3B10) else Color(0xFFFFF8E1)
    } else {
        Color.Transparent
    }
    val detailsStyle = UnifiedSpellListItemStyle.DetailsStyle(
        backgroundColor = if (isDarkTheme) Color(0xFF212121) else Color.White,
        paddingHorizontal = 8.dp,
        paddingVertical = 6.dp,
        dividerPadding = 4.dp,
        fontSize = 12.sp,
        labelWidth = 90.dp
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(rowBg).clickable { expanded = !expanded }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = spell.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (spell.castAction is CastAction.SpendInnateUsage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[ ● ]",
                            color = if (spell.isResourceExhausted) Color.LightGray else Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (spell.isConcentration) {
                        val cBg = if (spell.isCurrentConcentration) Color(0xFFFFD54F) else Color(0xFFE3F2FD)
                        val cFg = if (spell.isCurrentConcentration) Color(0xFF5D4037) else Color(0xFF1976D2)
                        Box(modifier = Modifier.padding(start = 6.dp).background(cBg, CircleShape).padding(horizontal = 4.dp)) {
                            Text("C", fontSize = 10.sp, color = cFg, fontWeight = FontWeight.Black)
                        }
                    }
                    if (spell.isRitual) {
                        Box(modifier = Modifier.padding(start = 4.dp).background(Color(0xFFE8F5E9), CircleShape).padding(horizontal = 4.dp)) {
                            Text("R", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                        }
                    }
                }
                    Text(text = spell.school, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                    spell.sourceTag?.let { tag ->
                        Text(text = tag, fontSize = 10.sp, color = Color(0xFFFFA726), fontWeight = FontWeight.SemiBold)
                    }
                    if (spell.castWarning != null) {
                        Text(text = spell.castWarning, fontSize = 11.sp, color = if (spell.isUpcast) Color(0xFFE64A19) else Color(0xFF0277BD), fontWeight = FontWeight.Medium)
                    }
            }
            if (spell.castAction != null) {
                IconButton(
                    onClick = { onCast(spell) },
                    enabled = !spell.isPending,
                    modifier = Modifier.size(36.dp).background(if (spell.isPending) Color.LightGray else Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                ) {
                    if (spell.isPending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Gray)
                    } else {
                        Icon(
                            imageVector = if (spell.castAction is CastAction.RitualIntent) Icons.Default.HourglassEmpty else Icons.Default.AutoAwesome,
                            contentDescription = "Cast",
                            tint = castIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            SpellDetailsBlock(
                castingTime = spell.castingTime,
                range = spell.range,
                components = spell.components,
                duration = spell.duration,
                description = spell.description,
                style = detailsStyle
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\components\SpellComponents.kt
