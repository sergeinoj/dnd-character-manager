package com.dnd.app.ui.screens.sheet.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.ui.theme.DndBonusGreen

@Composable
fun SquareInfoBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = if (isBonus && value.startsWith("+")) DndBonusGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatSquare(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueTextSize: TextUnit = 32.sp
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = valueTextSize, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
    }
}
