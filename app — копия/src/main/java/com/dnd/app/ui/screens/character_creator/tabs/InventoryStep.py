// Имя файла: ui/screens/character_creator/tabs/InventoryStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.WizardState
import com.dnd.app.ui.screens.character_creator.components.WizardSection

@Composable
fun InventoryStep(
    // ИСПРАВЛЕНИЕ: Добавлен Suppress, так как параметры нужны для единообразия API, но пока не используются
    @Suppress("UNUSED_PARAMETER") state: WizardState,
    @Suppress("UNUSED_PARAMETER") viewModel: CharacterCreatorViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        WizardSection("Стартовое снаряжение") {
            Text(
                "Персонаж получит стандартный набор снаряжения для своего класса и предыстории.",
                color = Color.White
            )
            Text(
                "\n(Функционал детального выбора снаряжения в разработке)",
                color = Color.Gray
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_creator/tabs/InventoryStep.kt