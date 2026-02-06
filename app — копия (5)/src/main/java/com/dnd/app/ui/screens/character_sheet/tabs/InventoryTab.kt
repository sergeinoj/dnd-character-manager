// Имя файла: ui/screens/character_sheet/tabs/InventoryTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Weapon

@Composable
fun InventoryTab(
    items: List<Weapon> // В будущем можно сделать общий интерфейс Item
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Рюкзак", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (items.isEmpty()) {
            Text("Пусто", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.weight} фнт. | ${item.cost}") },
                        trailingContent = { Text("1 шт.") }
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/InventoryTab.kt