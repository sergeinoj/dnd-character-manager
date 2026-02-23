// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CharacterCreatorScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.debug.DraftInspectorSheet
import com.dnd.app.ui.screens.character_creator.tabs.*
import com.dnd.app.ui.theme.DndBackground
import com.dnd.app.ui.theme.DndPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterCreatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInspector by remember { mutableStateOf(false) }
    val navContentColor = if (DndPrimary.luminance() > 0.5f) Color.Black else Color.White
    val navSelectedContentColor = if (navContentColor == Color.Black) Color.White else Color.Black
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = navSelectedContentColor,
        selectedTextColor = navContentColor,
        unselectedIconColor = navContentColor.copy(alpha = 0.9f),
        unselectedTextColor = navContentColor.copy(alpha = 0.9f),
        indicatorColor = navContentColor
    )

    val tabs = listOf(
        CreatorTab("Раса", Icons.Default.Person),
        CreatorTab("Класс", Icons.Default.AccountBox),
        CreatorTab("Статы", Icons.Default.Build),
        CreatorTab("Био", Icons.Default.Face),
        CreatorTab("Вещи", Icons.Default.ShoppingCart)
    )

    if (showInspector) {
        Dialog(onDismissRequest = { showInspector = false }) {
            DraftInspectorSheet(draft = state.draft, onDismiss = { showInspector = false })
        }
    }

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = if (state.draft.name.isBlank()) "Новый герой" else "${state.draft.name} (Ур. ${state.draft.levelStack.size})",
                onBack = onNavigateBack,
                onActionClick = { viewModel.saveCharacter(onSuccess = onNavigateBack) },
                actionIcon = { Icon(Icons.Default.Check, null, tint = Color.Black) },
                isActionEnabled = state.validationIssues.isEmpty(),
                onDebugClick = { showInspector = true },

                level = state.editingLevelIndex + 1,
                onLevelChange = { newLevel ->

                    if (newLevel <= state.draft.levelStack.size) {
                        viewModel.selectLevelToEdit(newLevel - 1)
                    } else if (newLevel == state.draft.levelStack.size + 1) {
                        viewModel.addLevel()
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DndPrimary) {
                tabs.forEachIndexed { index, tab ->
                    val hasError = state.tabErrors[index] == true
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = navItemColors,
                        label = { Text(tab.title) },
                        icon = {
                            BadgedBox(badge = { if (hasError) Badge() }) {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        }
                    )
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                val isExpanded: (String) -> Boolean = { key -> state.expandedStates[key] ?: false }
                val onToggle: (String) -> Unit = viewModel::toggleExpandedState

                when (selectedTab) {
                    0 -> RaceStep(state, viewModel, isExpanded, onToggle)
                    1 -> ClassStep(state, viewModel, isExpanded, onToggle)
                    2 -> StatsStep(
                        scores = state.draft.baseInfo.baseAbilityScores,
                        aggregateBonuses = state.draft.baseInfo.aggregateStatBonuses,
                        onStatChange = viewModel::updateStat
                    )
                    3 -> BioStep(state, viewModel, isExpanded, onToggle)
                    4 -> InventoryStep(state = state, viewModel = viewModel)
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CharacterCreatorScreen.kt
