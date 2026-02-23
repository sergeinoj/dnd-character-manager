// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\components\StatsComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.ui.theme.DndBonusGreen


@Composable
fun SquareInfoBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false,
    showDetailsHint: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isBonus && value.startsWith("+")) DndBonusGreen else MaterialTheme.colorScheme.onSurface
            )
        }
        if (showDetailsHint) {
            Text(
                text = "⋮",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 1f)
            )
        }
    }
}


@Composable
fun HealthButton(
    text: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\components\StatsComponents.kt
