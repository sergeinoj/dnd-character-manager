package com.dnd.app.ui.screens.sheet.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.dnd.app.domain.model.Money
import com.dnd.app.ui.theme.DndBonusGreen
import com.dnd.app.ui.theme.DndMalusRed

@Composable
fun HpModifierDialog(title: String, onDismiss: () -> Unit, onConfirmDamage: (Int) -> Unit, onConfirmHeal: (Int) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = amountText, onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it }, label = { Text("Количество") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onConfirmDamage(amountText.toIntOrNull() ?: 0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DndMalusRed)) { Text("УРОН") }; Button(onClick = { onConfirmHeal(amountText.toIntOrNull() ?: 0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DndBonusGreen)) { Text("ЛЕЧЕНИЕ") } } }, dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА") } })
}

@Composable
fun TempHpDialog(title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text("Количество") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }, confirmButton = { Button(onClick = { onConfirm(amount.toIntOrNull() ?: 0) }) { Text("OK") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА") } })
}

@Composable
fun UpcastPickerDialog(baseLevel: Int, availableSlots: Map<Int, Int>, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Усиление заклинания") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { (baseLevel..9).forEach { level -> val count = availableSlots[level] ?: 0; Button(onClick = { onConfirm(level) }, enabled = count > 0, modifier = Modifier.fillMaxWidth()) { Text("${level} уровень (${count} ячеек)") } } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА") } })
}

@Composable
fun ItemActionDialog(title: String, totalQuantity: Int, confirmButtonText: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit, pricePerUnit: Money? = null) {
    var amount by remember { mutableIntStateOf(1) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Всего: $totalQuantity"); Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { if (amount > 1) amount-- }) { Text("-", fontSize = 24.sp) }; Text(amount.toString(), fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp)); IconButton(onClick = { if (amount < totalQuantity) amount++ }) { Text("+", fontSize = 24.sp) } }; pricePerUnit?.let { Text("Итого: ${it * amount}", color = DndBonusGreen, fontWeight = FontWeight.Bold) } } }, confirmButton = { Button(onClick = { onConfirm(amount) }) { Text(confirmButtonText) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА") } })
}
