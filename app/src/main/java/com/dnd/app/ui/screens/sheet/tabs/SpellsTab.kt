// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\SpellsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.sheet.CharacterSheetUiState
import com.dnd.app.ui.screens.sheet.components.GlobalSlotsSection
import com.dnd.app.ui.screens.sheet.components.MagicSourceCard
import com.dnd.app.ui.screens.sheet.magic.*


@Composable
fun SpellsTab(
    state: CharacterSheetUiState,
    onCast: (SpellUiModel) -> Unit,
    onSpendSlot: (Int, Boolean) -> Unit,
    onOpenPreparation: (String) -> Unit,
    onToggleSpell: (String) -> Unit,
    onLearnSpell: (String) -> Unit,
    onConfirmPreparation: () -> Unit,
    onCancelPreparation: () -> Unit
) {
    val magicData = state.data?.magic ?: return
    val hasAnyMagicData = magicData.globalSlots != null || magicData.sources.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasAnyMagicData) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Персонаж не владеет магией", color = Color.LightGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                magicData.globalSlots?.takeIf { it.isVisible }?.let { slotsModel ->
                    item(key = "global_slots") {
                        GlobalSlotsSection(
                            model = slotsModel,
                            onSpendSlot = onSpendSlot,
                        )
                    }
                }

                items(magicData.sources, key = { it.id }) { source ->
                    MagicSourceCard(
                        source = source,
                        pendingActions = state.pendingActions,
                        onPrepare = { onOpenPreparation(source.id) },
                        onCast = onCast
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        magicData.activePreparation?.let { prepModel ->
            PreparationModal(
                model = prepModel,
                onToggleSpell = onToggleSpell,
                onLearnSpell = onLearnSpell,
                onConfirm = onConfirmPreparation,
                onCancel = onCancelPreparation
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\SpellsTab.kt
