package com.dnd.app.ui.screens.sheet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuantityStepper(quantity: Int, onUpdate: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Button(onClick = { onUpdate(quantity - 1) }, modifier = Modifier.size(24.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
        Text(text = quantity.toString(), modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Button(onClick = { onUpdate(quantity + 1) }, modifier = Modifier.size(24.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
    }
}

@Composable
fun SlotDot(color: Color, filled: Boolean) {
    Box(modifier = Modifier
        .size(14.dp)
        .border(1.dp, color, CircleShape)
        .background(if (filled) color else Color.Transparent, CircleShape))
}
