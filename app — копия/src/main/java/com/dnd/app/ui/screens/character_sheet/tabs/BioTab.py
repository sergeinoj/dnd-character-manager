// Имя файла: ui/screens/character_sheet/tabs/BioTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Bio

@Composable
fun BioTab(bio: Bio) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BioField("Черты характера", bio.traits)
        BioField("Идеалы", bio.ideals)
        BioField("Привязанности", bio.bonds)
        BioField("Слабости", bio.flaws)
        BioField("Предыстория", bio.background)
        BioField("Заметки", bio.notes)
    }
}

@Composable
fun BioField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {}, // Read-only пока
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true
    )
    Spacer(modifier = Modifier.height(8.dp))
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/BioTab.kt