package com.dnd.app.ui.screens.sheet.components.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.Money

@Composable
fun MoneyWidget(coins: Money, onUpdate: (String, Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) { MoneyRow("ЗМ", coins.gp, "GP", onUpdate); MoneyRow("СМ", coins.sp, "SP", onUpdate); MoneyRow("ММ", coins.cp, "CP", onUpdate) }
}

@Composable
private fun MoneyRow(label: String, currentValue: Int, type: String, onUpdate: (String, Int) -> Unit) {
    var calcValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(34.dp))
        Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp), contentAlignment = Alignment.Center) {
            Text(currentValue.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = calcValue,
            onValueChange = { if (it.all { c -> c.isDigit() }) calcValue = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.width(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                .border(1.dp, Color.Gray)
                .padding(vertical = 4.dp)
        )
        Spacer(Modifier.width(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(modifier = Modifier.size(32.dp).clickable { val a = calcValue.toIntOrNull() ?: 0; if (a > 0) onUpdate(type, -a); calcValue = "" }, color = Color(0xFFBDBDBD)) { Box(contentAlignment = Alignment.Center) { Text("-", color = MaterialTheme.colorScheme.onSurface) } }
            Surface(modifier = Modifier.size(32.dp).clickable { val a = calcValue.toIntOrNull() ?: 0; if (a > 0) onUpdate(type, a); calcValue = "" }, color = Color.DarkGray) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White) } }
        }
    }
}
