// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\BioTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.sheet.AutoLoreSection

@Composable
fun BioTab(
    autoLore: List<AutoLoreSection>,
    manualBioFields: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        autoLore.forEach { section ->
            LoreAccordion(section)
        }
        if (manualBioFields.isNotEmpty()) {
            ManualBioSection(manualBioFields)
        }
    }
}

@Composable
private fun ManualBioSection(fields: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Личные заметки", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            fields.forEach { (label, value) ->
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(value, modifier = Modifier.padding(top = 2.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LoreAccordion(section: AutoLoreSection) {
    var expanded by remember(section.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Text(section.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    section.blocks.forEach { block ->
                        Text(block.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            block.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\BioTab.kt
