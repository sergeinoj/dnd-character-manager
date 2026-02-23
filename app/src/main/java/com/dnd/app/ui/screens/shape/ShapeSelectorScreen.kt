package com.dnd.app.ui.screens.shape

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.ui.components.DndTopBar
import com.dnd.app.util.DndLocalization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeSelectorScreen(
    navigateUp: () -> Unit,
    viewModel: ShapeSelectorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val errorMessage = state.error
    var previewMonster by remember { mutableStateOf<MonsterRecord?>(null) }
    val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTransforming = state.isLoading && state.monsters.isNotEmpty()

    Scaffold(
        topBar = {
            DndTopBar(
                title = "Подбор зверя",
                canNavigateBack = true,
                navigateUp = navigateUp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterRow(
                selected = state.selectedRange,
                onSelect = viewModel::onFilterSelected
            )

            when {
                state.isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.monsters, key = { it.index }) { monster ->
                            ShapeRow(
                                monster = monster,
                                enabled = !state.isLoading,
                                onSelect = { previewMonster = monster }
                            )
                        }
                    }
                }
            }
        }
    }

    if (previewMonster != null) {
        ModalBottomSheet(
            onDismissRequest = { previewMonster = null },
            sheetState = previewSheetState
        ) {
            MonsterPreviewSheet(
                monster = previewMonster!!,
                isTransforming = isTransforming,
                onTransform = {
                    val selectedMonster = previewMonster ?: return@MonsterPreviewSheet
                    viewModel.onShapeSelected(selectedMonster.index) {
                        previewMonster = null
                        navigateUp()
                    }
                },
                onClose = { previewMonster = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(selected: CrRange, onSelect: (CrRange) -> Unit) {
    val ranges = CrRange.values()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter icon")
        ranges.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selected == range) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = if (selected == range) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
private fun ShapeRow(
    monster: MonsterRecord,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled, onClick = onSelect)
    ) {
        Text(
            text = monster.name,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                append("CR ${monster.challengeRating ?: "—"}")
                monster.size?.let { append(" · ${DndLocalization.translateMonsterSize(it)}") }
                monster.type?.let { append(" · ${DndLocalization.translateMonsterType(it)}") }
                monster.alignment?.let { append(" · ${DndLocalization.translateAlignment(it)}") }
            },
            style = MaterialTheme.typography.bodySmall
        )

        if (monster.languages.isNotEmpty()) {
            Text(
                text = "Языки: ${monster.languages.joinToString { DndLocalization.translateProficiency(it) }}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (monster.senses.isNotEmpty()) {
            Text(
                text = "Чувства: " + monster.senses.entries.joinToString { "${DndLocalization.translateSenseKey(it.key)}: ${it.value}" },
                style = MaterialTheme.typography.bodySmall
            )
        }
        Divider(modifier = Modifier.padding(top = 6.dp))
    }
}




