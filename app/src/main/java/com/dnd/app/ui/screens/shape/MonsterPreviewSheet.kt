package com.dnd.app.ui.screens.shape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.MonsterAction
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.ui.screens.sheet.components.common.StatSquare
import com.dnd.app.util.DndLocalization

@Composable
fun MonsterPreviewSheet(
    monster: MonsterRecord,
    isTransforming: Boolean,
    onTransform: () -> Unit,
    onClose: () -> Unit
) {
    val speedLabel = monster.speed.entries.joinToString(" / ") {
        "${DndLocalization.translateSpeed(it.key)}: ${it.value}"
    }
    val details = listOfNotNull(
        monster.size?.let(DndLocalization::translateMonsterSize),
        monster.type?.let(DndLocalization::translateMonsterType),
        monster.alignment?.let(DndLocalization::translateAlignment)
    ).joinToString(" · ")
    val orderedStats = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(monster.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    monster.challengeRating?.let { if (it % 1.0 == 0.0) "CR ${it.toInt()}" else "CR $it" } ?: "CR —",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatSquare(modifier = Modifier.weight(1f), title = "БА", value = monster.armorClass?.toString() ?: "—")
            StatSquare(modifier = Modifier.weight(1f), title = "ХП", value = monster.hitPoints?.toString() ?: "—")
            StatSquare(
                modifier = Modifier.weight(1f),
                title = "Скорость",
                value = speedLabel.ifBlank { "—" },
                valueTextSize = 10.sp
            )
        }

        if (monster.stats.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                orderedStats.forEach { code ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(DndLocalization.translateStat(code), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(monster.stats[code]?.toString() ?: "—", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }

        if (!monster.description.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Описание", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(monster.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (monster.languages.isNotEmpty()) {
            Text(
                "Языки: ${monster.languages.joinToString { DndLocalization.translateProficiency(it) }}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (monster.senses.isNotEmpty()) {
            Text(
                "Чувства: " + monster.senses.entries.joinToString { "${DndLocalization.translateSenseKey(it.key)}: ${it.value}" },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (monster.actions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Действия", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                monster.actions.forEach { action -> ActionRow(action) }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onTransform,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTransforming
        ) {
            Text("Трансформироваться")
        }

        if (isTransforming) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ActionRow(action: MonsterAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(action.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                val toHit = action.attackBonus?.let { if (it >= 0) "+$it" else "$it" } ?: "—"
                Text("Бонус: $toHit", style = MaterialTheme.typography.bodyMedium)
            }
            val damage = action.damage.joinToString(" + ") { listOfNotNull(it.dice, it.type).joinToString(" ") }.ifBlank { "—" }
            Text("Урон: $damage", style = MaterialTheme.typography.bodySmall)
            Text("Дальность: ${action.range ?: "—"}", style = MaterialTheme.typography.bodySmall)
            if (!action.description.isNullOrBlank()) {
                Divider()
                Text(action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}




