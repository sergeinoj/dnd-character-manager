// Имя файла: ui/screens/character_sheet/CharacterSheetScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_sheet.tabs.BioTab
import com.dnd.app.ui.screens.character_sheet.tabs.CombatTab
import com.dnd.app.ui.screens.character_sheet.tabs.InventoryTab
import com.dnd.app.ui.screens.character_sheet.tabs.SkillsTab
import com.dnd.app.ui.screens.character_sheet.tabs.SpellsTab
import com.dnd.app.ui.screens.character_sheet.tabs.StatsTab
import com.dnd.app.ui.theme.DndBackground

@Composable
fun CharacterSheetScreen(
    navigateUp: () -> Unit,
    viewModel: CharacterSheetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(3) }

    val tabs = listOf(
        TabItem("Магия", Icons.Filled.Star),
        TabItem("Бой", Icons.Filled.Build),
        TabItem("Снаряж.", Icons.Filled.ShoppingCart),
        TabItem("Главная", Icons.Filled.AccountBox),
        TabItem("Навыки", Icons.Filled.List),
        TabItem("Личность", Icons.Filled.Face),
        TabItem("Заметки", Icons.Filled.Create)
    )

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = state.character?.name ?: "...",
                onBack = navigateUp
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.LightGray)
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, item ->
                    CustomBottomNavItem(
                        item = item,
                        isSelected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        if (state.isLoading || state.character == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val char = state.character!!

            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTabIndex) {
                    0 -> SpellsTab(state.spells)
                    1 -> CombatTab(state.weapons)
                    2 -> InventoryTab(state.weapons)
                    3 -> StatsTab(
                        character = char,
                        calculator = viewModel.calculator,
                        onHpChange = { delta -> viewModel.updateHp(delta) },
                        onMoneyChange = { type, delta -> viewModel.updateMoney(type, delta) }
                    )
                    4 -> SkillsTab(
                        character = char,
                        calculator = viewModel.calculator
                    )
                    5 -> BioTab(char.bio)
                    6 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Заметки (WIP)", color = Color.White) }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun CustomBottomNavItem(
    item: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color.White else Color.LightGray

    Column(
        modifier = Modifier
            .run { if (isSelected) height(60.dp) else height(56.dp) }
            .background(bg)
            .border(1.dp, Color.Gray)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Text(
            text = item.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/CharacterSheetScreen.kt