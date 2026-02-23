// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\components\HitDiceRecoveryDialog.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.screens.sheet.HitDicePoolView

@Composable
fun HitDiceRecoveryDialog(
    visible: Boolean,
    hitDicePools: List<HitDicePoolView>,
    remainingDice: Int,
    totalDice: Int,
    hitDiceFormula: String,
    onSpendDie: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val hasDiceLeft = remainingDice > 0

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Трата костей хитов") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Доступно $remainingDice ${pluralize(remainingDice, "кость", "кости", "костей")} из $totalDice")
                if (hitDicePools.isEmpty()) {
                    Text(
                        "Формула костей: ${hitDiceFormula.ifBlank { "не указана" }}"
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        hitDicePools.forEach { pool ->
                            val remainingText = if (pool.total == pool.remaining) "${pool.remaining} ${pluralize(pool.remaining, "кость", "кости", "костей")}" else "${pool.remaining}/${pool.total}"
                            Button(
                                onClick = { onSpendDie(pool.dieType) },
                                enabled = pool.remaining > 0,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("d${pool.dieType} ($remainingText)")
                            }
                        }
                    }
                }
                if (!hasDiceLeft) {
                    Text(
                        text = "Костей хитов больше нет.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Закрыть")
            }
        }
    )
}

private fun pluralize(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    if (mod100 in 11..14) return many
    return when (count % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}
// --- КОНЕЦ ФАЙЛА ---
