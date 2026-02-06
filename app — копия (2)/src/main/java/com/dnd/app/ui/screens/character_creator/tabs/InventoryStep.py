// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InventoryStep(
    className: String?,
    backgroundName: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Стартовое снаряжение", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Ваше снаряжение определяется выбранным классом и предысторией.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Блок класса
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("От класса: ${className ?: "Не выбран"}", fontWeight = FontWeight.Bold)
                if (className != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("• Стандартный набор экипировки для $className (будет добавлено в инвентарь).", fontSize = 14.sp)
                    // В будущем здесь будет парсинг JSON с вариантами (А или Б)
                }
            }
        }

        // Блок предыстории
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("От предыстории: ${backgroundName ?: "Не выбрана"}", fontWeight = FontWeight.Bold)
                if (backgroundName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("• Набор одежды", fontSize = 14.sp)
                    Text("• Кошель с монетами", fontSize = 14.sp)
                    Text("• Профессиональный инструмент (если есть)", fontSize = 14.sp)
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt