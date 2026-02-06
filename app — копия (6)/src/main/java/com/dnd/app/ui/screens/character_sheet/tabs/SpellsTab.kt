// Имя файла: ui/screens/character_sheet/tabs/SpellsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Spell

@Composable
fun SpellsTab(
    spells: List<Spell>
) {
    if (spells.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Книга заклинаний пуста.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(spells) { spell ->
                SpellCard(spell)
            }
        }
    }
}

@Composable
fun SpellCard(spell: Spell) {
    Card(
        modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(spell.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${spell.level} круг, ${spell.school}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            Text("Время: ${spell.castingTime} | Дистанция: ${spell.range}", style = MaterialTheme.typography.bodySmall)
            Text(spell.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/SpellsTab.kt