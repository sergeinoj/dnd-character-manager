package com.dnd.app.ui.screens.sheet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.sheet.SenseTrait

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensesLayerSheet(
    visible: Boolean,
    senses: List<SenseTrait>,
    languages: List<String>,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(visible) {
        if (visible) sheetState.show()
    }

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Слои чувств", style = MaterialTheme.typography.titleLarge)
            Text(
                "Тёмное зрение, слепое зрение, языки и другие сенсорные бонусы героя.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SenseSection(title = "Чувства", entries = senses)
            Divider()
            LanguageSection(languages = languages)

            if (senses.isEmpty() && languages.isEmpty()) {
                Text(
                    "Пока нет обнаруженных чувств или языков.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SenseSection(title: String, entries: List<SenseTrait>) {
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
                    Text("\u2022 ${entry.title}", style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun LanguageSection(languages: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Языки (${languages.size})", style = MaterialTheme.typography.titleMedium)
        if (languages.isEmpty()) {
            Text(
                "Языки пока не указаны.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            languages.forEach { lang ->
                Text("\u2022 $lang", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
