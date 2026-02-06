// Имя файла: ui/screens/character_creator/tabs/BioStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.WizardState
import com.dnd.app.ui.screens.character_creator.components.WizardSection
import com.dnd.app.ui.theme.DndSurface

@Composable
fun BioStep(
    state: WizardState,
    viewModel: CharacterCreatorViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WizardSection("Личные данные") {
            BioInput(
                value = state.draft.name,
                onValueChange = { viewModel.updateName(it) },
                label = "Имя персонажа *",
                isError = state.draft.name.isBlank()
            )
            
            // В будущем добавить остальные поля в DraftCharacter и ViewModel
            // BioInput(value = "", onValueChange = {}, label = "Мировоззрение")
            // BioInput(value = "", onValueChange = {}, label = "Предыстория")
        }
    }
}

@Composable
private fun BioInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DndSurface,
            unfocusedContainerColor = DndSurface,
            errorContainerColor = Color(0xFFFFCDD2),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        isError = isError
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_creator/tabs/BioStep.kt