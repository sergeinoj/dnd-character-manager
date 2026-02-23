package com.dnd.app.ui.screens.sheet.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.sheet.DefenseTrait

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefenseLayerSheet(
    visible: Boolean,
    isTransformed: Boolean,
    heroResistances: List<DefenseTrait>,
    heroImmunities: List<DefenseTrait>,
    beastResistances: List<DefenseTrait>,
    beastImmunities: List<DefenseTrait>,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(visible) {
        if (visible) sheetState.show()
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Слои защиты (КД)", style = MaterialTheme.typography.titleLarge)
            Text(
                "Детализация сопротивлений и иммунитетов по активным эффектам",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DefenseSection(title = "Герой · Сопротивления", entries = heroResistances)
            Divider()
            DefenseSection(title = "Герой · Иммунитеты", entries = heroImmunities)
            if (isTransformed) {
                Divider()
                DefenseSection(title = "Зверь · Сопротивления", entries = beastResistances)
                Divider()
                DefenseSection(title = "Зверь · Иммунитеты", entries = beastImmunities)
            }

            val noHeroDefense = heroResistances.isEmpty() && heroImmunities.isEmpty()
            val noBeastDefense = beastResistances.isEmpty() && beastImmunities.isEmpty()
            val showEmptyState = if (isTransformed) (noHeroDefense && noBeastDefense) else noHeroDefense
            if (showEmptyState) {
                Text(
                    "Дополнительных защитных свойств не найдено",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DefenseSection(title: String, entries: List<DefenseTrait>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$title (${entries.size})", style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) {
            Text(
                "Нет данных",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entries.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("• ${entry.title}", style = MaterialTheme.typography.bodyLarge)
                    entry.detail?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
