// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\components\Inputs.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator

@Composable
fun StatInput(
    name: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    calculator: DndCalculator
) {
    val modifier = calculator.calculateModifier(value)
    val modString = calculator.formatModifier(modifier)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )


        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }


            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {

                Text(
                    text = modString,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }


            IconButton(
                onClick = { if (value < 30) onValueChange(value + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }


    val displayText = if (options.isNotEmpty() && selectedIndex in options.indices) {
        options[selectedIndex]
    } else {
        "Выберите..."
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            readOnly = true,
            value = displayText,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption) },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\components\Inputs.kt