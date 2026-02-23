// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\NotesTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun NotesTab(
    logs: List<String>,
    notes: String,
    onUpdate: (String) -> Unit
) {

    var localNotes by remember { mutableStateOf(notes) }


    LaunchedEffect(notes) {
        if (notes != localNotes) {
            localNotes = notes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (logs.isNotEmpty()) {
            Text(
                text = "ЖУРНАЛ СОБЫТИЙ",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .background(Color(0xFF212121), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = localNotes,
            onValueChange = {
                localNotes = it
                onUpdate(it)
            },
            modifier = Modifier.fillMaxSize(),
            label = { Text("Ваши заметки") },
            placeholder = { Text("Опишите ваши приключения...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\NotesTab.kt
