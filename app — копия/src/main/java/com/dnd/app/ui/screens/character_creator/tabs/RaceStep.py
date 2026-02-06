// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.components.DndDropdown
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.WizardState
import com.dnd.app.ui.screens.character_creator.components.WizardSection

@Composable
fun RaceStep(state: WizardState, viewModel: CharacterCreatorViewModel) {
    val draft = state.draft
    val selectedRaceIdx = if (draft.raceId != null) state.availableRaces.indexOfFirst { it.id == draft.raceId }.coerceAtLeast(0) else 0
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WizardSection("Выбор происхождения") {
            val labelText = if (state.isLoading) "Загрузка..." else if (state.availableRaces.isEmpty()) "Нет данных (БД пуста)" else "Раса"
            val options = if (state.availableRaces.isNotEmpty()) state.availableRaces.map { it.name } else listOf("Список пуст")
            DndDropdown(label = labelText, options = options, selectedIndex = selectedRaceIdx,
                onOptionSelected = { if (state.availableRaces.isNotEmpty()) viewModel.selectRace(it) }
            )
        }
        if (draft.raceId != null) {
            WizardSection("Особенности расы") {
                Text("Бонусы характеристик:", color = Color.LightGray)
                if (draft.raceStats.isNotEmpty()) {
                    draft.raceStats.forEach { (stat, bonus) -> Text("• ${stat.replaceFirstChar { it.uppercase() }}: +$bonus", color = Color.White) }
                } else { Text("Нет автоматических бонусов", color = Color.White) }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt