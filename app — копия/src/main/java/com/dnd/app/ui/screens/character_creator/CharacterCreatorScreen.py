// Имя файла: ui/screens/character_creator/CharacterCreatorScreen.kt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_creator.tabs.*
import com.dnd.app.ui.theme.DndBackground
import com.dnd.app.ui.theme.DndPrimary

@Composable
fun CharacterCreatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterCreatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(1) }
    val tabs = listOf(
        CreatorTab("Статы", Icons.Default.Build), CreatorTab("Раса", Icons.Default.Person),
        CreatorTab("Класс", Icons.Default.AccountBox), CreatorTab("Био", Icons.Default.Face),
        CreatorTab("Вещи", Icons.Default.ShoppingCart)
    )
    Scaffold(
        topBar = { DndActionTopBar(title = if (state.draft.name.isBlank()) "Новый герой" else state.draft.name, onBack = onNavigateBack) },
        bottomBar = {
            NavigationBar(containerColor = DndPrimary) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index, onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }, label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = Color.Gray, unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.saveCharacter(onSuccess = onNavigateBack) },
                containerColor = if (state.isSaveEnabled && state.draft.pointsRemaining == 0) Color(0xFF4CAF50) else Color.Gray
            ) { Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White) }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> StatsStep(state, viewModel)
                1 -> RaceStep(state, viewModel)
                2 -> ClassStep(state, viewModel)
                3 -> BioStep(state, viewModel)
                4 -> InventoryStep(state, viewModel)
            }
        }
    }
}
data class CreatorTab(val title: String, val icon: ImageVector)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_creator/CharacterCreatorScreen.kt