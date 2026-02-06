// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt
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
fun ClassStep(state: WizardState, viewModel: CharacterCreatorViewModel) {
    val draft = state.draft
    val selectedClassIdx = if (draft.levels.isNotEmpty()) state.availableClasses.indexOfFirst { it.id == draft.levels.first().classId }.coerceAtLeast(0) else 0
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WizardSection("Основной класс") {
            val labelText = if (state.isLoading) "Загрузка..." else if (state.availableClasses.isEmpty()) "Нет данных (БД пуста)" else "Класс (1-й уровень)"
            val options = if (state.availableClasses.isNotEmpty()) state.availableClasses.map { it.name } else listOf("Список пуст")
            DndDropdown(label = labelText, options = options, selectedIndex = selectedClassIdx,
                onOptionSelected = { if (state.availableClasses.isNotEmpty()) viewModel.selectFirstClass(it) }
            )
        }
        if (draft.levels.isNotEmpty()) {
            val currentClass = state.availableClasses.find { it.id == draft.levels.first().classId }
            WizardSection("Детали класса") {
                Text("Кость хитов: d${currentClass?.hitDie ?: "?"}", color = Color.White)
                Text("\nСпасброски и Навыки будут доступны после выбора.", color = Color.Gray)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt