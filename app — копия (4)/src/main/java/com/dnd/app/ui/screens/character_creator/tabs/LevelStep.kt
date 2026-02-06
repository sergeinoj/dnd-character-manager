// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/LevelStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.LevelStep

@Composable
fun LevelStep(
    step: LevelStep,
    features: List<Feature>,
    onHpChange: (Int) -> Unit,
    onSelectionChange: (String, ChoiceResult) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Блок ХП
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Увеличение Здоровья (HP)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (step.hpIncrease == 0) "" else step.hpIncrease.toString(),
                            onValueChange = { onHpChange(it.toIntOrNull() ?: 0) },
                            modifier = Modifier.width(100.dp),
                            label = { Text("HP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Киньте кость хитов класса или возьмите среднее значение.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Блок Фич
        if (features.isEmpty()) {
            item {
                Text("На этом уровне нет новых способностей.", modifier = Modifier.padding(8.dp), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        } else {
            items(features) { feature ->
                FeatureCard(feature, step, onSelectionChange)
            }
        }
    }
}

@Composable
fun FeatureCard(
    feature: Feature,
    step: LevelStep,
    onSelectionChange: (String, ChoiceResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(feature.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            if (feature.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(feature.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            if (feature.choices.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Необходимо сделать выбор:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                
                feature.choices.forEach { _ ->
                    // Здесь будет сложный UI выбора (Dropdown, Checkboxes)
                    // Пока реализуем заглушку-кнопку для ТЗ
                    val isChosen = step.selections.containsKey(feature.index)
                    
                    Button(
                        onClick = { onSelectionChange(feature.index, ChoiceResult.Note("Choice Made")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isChosen) "Выбор сделан" else "Выбрать...")
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/LevelStep.kt