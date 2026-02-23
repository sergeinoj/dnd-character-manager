// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\PreparationModal.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.magic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun PreparationModal(
    model: PreparationStateUiModel,
    onToggleSpell: (String) -> Unit,
    onLearnSpell: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(enabled = model.isModified) {
        onCancel()
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null) }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(model.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Подготовлено: ${model.counterText}",
                                fontSize = 12.sp,
                                color = if (model.canConfirm) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = onConfirm,
                            enabled = model.canConfirm && model.isModified,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("СОХРАНИТЬ")
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.preparedSpells.isNotEmpty()) {
                    item { Text("ПОДГОТОВЛЕННЫЕ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(model.preparedSpells, key = { "prep_${it.id}" }) { spell ->
                        PrepSpellRow(spell, isSelected = true, onToggle = { onToggleSpell(spell.id) })
                    }
                }

                if (model.availableSpells.isNotEmpty()) {
                    item { Spacer(Modifier.height(16.dp)); Text("ДОСТУПНЫЕ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(model.availableSpells, key = { "avail_${it.id}" }) { spell ->
                        PrepSpellRow(
                            spell = spell,
                            isSelected = false,
                            onToggle = { onToggleSpell(spell.id) },
                            onLearn = if (model.canLearnSpells) ({ onLearnSpell(spell.id) }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrepSpellRow(
    spell: SpellUiModel,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLearn: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !spell.isAlwaysPrepared, onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spell.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${spell.level} уровень, ${spell.school}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (spell.isAlwaysPrepared) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isSelected && onLearn != null) {
                        TextButton(onClick = onLearn) { Text("Learn") }
                    }
                    Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\PreparationModal.kt
