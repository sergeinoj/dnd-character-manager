// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/ui/screens/sheet/magic/TacticalCastDialog.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dnd.app.domain.model.snapshot.CombatAction
import com.dnd.app.ui.screens.sheet.magic.calculateUpcastDamage

@Composable
fun TacticalCastDialog(
    action: CombatAction,
    globalSlots: GlobalSlotsUiModel?,
    onSpendSlot: (level: Int, isPact: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (globalSlots == null) return
    val startLevel = action.level ?: 1
            val classSlotRows = (startLevel..9).map { level ->
                val slotModel = globalSlots.classSlots.find { it.level == level }
                TacticalSlotRowModel(
                    level = level,
                    slotModel = slotModel,
                    damagePreview = calculateUpcastDamage(action, level),
                    isPact = false
                )
            }
    val pactSlots = globalSlots.pactSlots

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(18.dp)
            ) {
                TacticalDialogHeader(action)
                Spacer(Modifier.height(16.dp))

                TacticalSectionHeader(
                    title = "МАГИЯ КЛАССА",
                    accentColor = MaterialTheme.colorScheme.primary,
                    tag = "ДО"
                )
                if (classSlotRows.none { it.hasSlots }) {
                    Text("Нет доступных классических ячеек.", style = MaterialTheme.typography.bodySmall)
                } else {
                    classSlotRows.forEach { row ->
                        TacticalSlotRow(
                            rowModel = row,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onSpendSlot(row.level, false) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (pactSlots != null && pactSlots.level >= startLevel) {
                    TacticalSectionHeader(
                        title = "МАГИЯ ПАКТА",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        tag = "КО"
                    )
                    TacticalSlotRow(
                        rowModel = TacticalSlotRowModel(
                            level = pactSlots.level,
                            slotModel = pactSlots,
                            damagePreview = calculateUpcastDamage(action, pactSlots.level),
                            isPact = true
                        ),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        showRecovery = true,
                        onClick = { onSpendSlot(pactSlots.level, true) }
                    )
                }

                if (action.isRitual) {
                    Spacer(Modifier.height(20.dp))
                    RitualNote()
                }
            }
        }
    }
}

@Composable
private fun TacticalDialogHeader(action: CombatAction) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(action.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Базовый уровень ${action.level ?: 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TacticalSectionHeader(title: String, accentColor: Color, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(tag, color = accentColor, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = accentColor)
    }
}

@Composable
private fun TacticalSlotRow(
    rowModel: TacticalSlotRowModel,
    accentColor: Color,
    showRecovery: Boolean = false,
    onClick: () -> Unit
) {
    val hasSlots = rowModel.hasSlots
    val availableCount = rowModel.availableCount
    val pendingCount = rowModel.pendingCount
    val isEnabled = availableCount > 0

    Surface(
        color = if (isEnabled) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = isEnabled && hasSlots, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rowModel.level} уровень",
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = rowModel.damagePreview,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hasSlots) "Остаток: $availableCount${if (pendingCount > 0) " (в ожидании $pendingCount)" else ""}" else "Нет ячеек",
                    style = MaterialTheme.typography.bodySmall
                )
                if (showRecovery) {
                    Text(
                        text = "Восстанавливается в КО",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun RitualNote() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "ritual",
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(10.dp))
            Text("Время каста: +10 мин. Ячейка не тратится", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class TacticalSlotRowModel(
    val level: Int,
    val slotModel: SpellSlotLevelUiModel?,
    val damagePreview: String,
    val isPact: Boolean
) {
    val hasSlots: Boolean = slotModel?.slots?.isNotEmpty() == true
    val availableCount: Int = slotModel?.slots?.count { !it.isSpent && !it.isPending } ?: 0
    val pendingCount: Int = slotModel?.slots?.count { it.isPending } ?: 0
}

// --- КОНЕЦ ФАЙЛА ---
