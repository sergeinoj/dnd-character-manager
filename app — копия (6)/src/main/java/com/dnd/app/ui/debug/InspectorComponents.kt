// Имя файла: app/src/main/java/com/dnd/app/ui/debug/InspectorComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle // ИСПРАВЛЕНО: Добавлен импорт
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.DraftCharacter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Обертка для каждой секции инспектора.
 */
@Composable
fun InspectorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * Стандартная строка для отображения пары "ключ: значение".
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f),
            fontSize = 13.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.6f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Визуализатор дерева выборов, сделанных пользователем.
 */
@Composable
fun SelectionTreeView(
    title: String,
    selections: Map<String, ChoiceResult>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        if (selections.isEmpty()) {
            Text("  - None", color = Color.Gray, fontSize = 12.sp)
        } else {
            selections.forEach { (key, result) ->
                val resultString = when (result) {
                    is ChoiceResult.Skills -> "(Skills) ${result.skillIndexes.joinToString()}"
                    is ChoiceResult.Spells -> "(Spells) ${result.spellIndexes.joinToString()}"
                    is ChoiceResult.SelectedOptions -> "(Options) ${result.items.joinToString()}"
                    is ChoiceResult.StatBonus -> "(Stats) ${result.bonuses.map { "${it.key}:+${it.value}" }.joinToString()}"
                    is ChoiceResult.Note -> "(Note) ${result.text}"
                    is ChoiceResult.RuleEffect -> "(Effect) ${result.effectType}: ${result.value}"
                }
                Text(
                    "• $key -> $resultString",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Компонент для отображения сырого JSON объекта DraftCharacter.
 */
@Composable
fun RawJsonView(draft: DraftCharacter) {
    val json = remember { Json { prettyPrint = true; encodeDefaults = true } }
    val jsonString = remember(draft) {
        try {
            json.encodeToString(draft)
        } catch (e: Exception) {
            "Error serializing DraftCharacter: ${e.message}"
        }
    }

    OutlinedTextField(
        value = jsonString,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        label = { Text("Serialized State") },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/debug/InspectorComponents.kt