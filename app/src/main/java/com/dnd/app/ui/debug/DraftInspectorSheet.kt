// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\debug\DraftInspectorSheet.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.DraftCharacter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftInspectorSheet(
    draft: DraftCharacter,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draft Inspector (Debug)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Inspector")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFECEFF1)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InspectorSection(title = "Identity & Base") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(label = "Name", value = draft.name.ifBlank { "N/A" })
                        InfoRow(label = "Race Index", value = draft.baseInfo.raceIndex.ifBlank { "N/A" })
                        InfoRow(label = "Subrace Index", value = draft.baseInfo.subraceIndex ?: "N/A")
                        InfoRow(label = "Background Index", value = draft.baseInfo.backgroundIndex.ifBlank { "N/A" })
                        InfoRow(label = "Alignment Index", value = draft.baseInfo.alignmentIndex.ifBlank { "N/A" })

                        Text("Static Proficiencies (Hard Exclusions):", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                        if (draft.baseInfo.staticProficiencies.isEmpty()) {
                            Text("- None", color = Color.Gray)
                        } else {
                            draft.baseInfo.staticProficiencies.forEach {
                                Text("- $it")
                            }
                        }
                    }
                }
            }

            item {
                InspectorSection(title = "Ability Score Engine") {
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Base (Point-Buy)", fontWeight = FontWeight.Medium)
                            draft.baseInfo.baseAbilityScores.toSortedMap().forEach { (stat, value) ->
                                InfoRow(label = stat, value = value.toString())
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aggregate Bonuses", fontWeight = FontWeight.Medium)
                            draft.baseInfo.aggregateStatBonuses.toSortedMap().forEach { (stat, value) ->
                                InfoRow(label = stat, value = "+$value")
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Final Values", fontWeight = FontWeight.Medium)
                            draft.baseInfo.baseAbilityScores.toSortedMap().forEach { (stat, value) ->
                                val bonus = draft.baseInfo.aggregateStatBonuses[stat] ?: 0
                                InfoRow(label = stat, value = "${value + bonus}")
                            }
                        }
                    }
                }
            }

            item {
                InspectorSection(title = "Selection Tree") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectionTreeView(title = "Race Selections", selections = draft.baseInfo.raceSelections)
                        SelectionTreeView(title = "Background Selections", selections = draft.baseInfo.backgroundSelections)
                        draft.levelStack.forEachIndexed { index, levelStep ->
                            SelectionTreeView(
                                title = "Level ${index + 1} (${levelStep.classIndex}) Selections",
                                selections = levelStep.selections
                            )
                        }
                    }
                }
            }

            item {
                InspectorSection(title = "Raw JSON Data") {
                    RawJsonView(draft = draft)
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\debug\DraftInspectorSheet.kt