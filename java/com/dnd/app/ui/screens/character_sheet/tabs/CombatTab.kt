// Имя файла: ui/screens/character_sheet/tabs/CombatTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Weapon

@Composable
fun CombatTab(
    weapons: List<Weapon>
) {
    if (weapons.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("В руках пусто. Добавьте оружие в инвентарь.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(weapons) { weapon ->
                WeaponCard(weapon)
            }
        }
    }
}

@Composable
fun WeaponCard(weapon: Weapon) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(weapon.name, fontWeight = FontWeight.Bold)
                Text(weapon.damage, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Text("Тип: ${weapon.damageType}", style = MaterialTheme.typography.bodySmall)
            if (weapon.properties.isNotEmpty()) {
                Text("Св-ва: ${weapon.properties}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/CombatTab.kt