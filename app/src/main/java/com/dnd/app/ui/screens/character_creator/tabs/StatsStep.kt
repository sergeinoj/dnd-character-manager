// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\StatsStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.rules.DndRules

@Composable
fun StatsStep(
    scores: Map<String, Int>,
    aggregateBonuses: Map<String, Int>,
    onStatChange: (String, Int) -> Unit
) {
    val spent = scores.values.sumOf { DndRules.getPointCost(it) }
    val remaining = DndRules.MAX_POINTS - spent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF424242), RoundedCornerShape(4.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Осталось очков", color = Color.White, fontSize = 12.sp)
                Text(
                    text = "$remaining",
                    color = if (remaining >= 0) Color.White else Color.Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))


        val stats = listOf(
            "Сила" to "STR", "Ловкость" to "DEX",
            "Телосложение" to "CON", "Интеллект" to "INT",
            "Мудрость" to "WIS", "Харизма" to "CHA"
        )

        stats.chunked(2).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowStats.forEach { (name, key) ->
                    StatSquareItem(
                        modifier = Modifier.weight(1f),
                        name = name,
                        boughtValue = scores[key] ?: 8,
                        totalBonus = aggregateBonuses[key] ?: 0,
                        onValueChange = { diff -> onStatChange(key, diff) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatSquareItem(
    modifier: Modifier,
    name: String,
    boughtValue: Int,
    totalBonus: Int,
    onValueChange: (Int) -> Unit
) {
    val totalValue = boughtValue + totalBonus
    val modifierValue = (totalValue - 10) / 2
    val modSign = if (modifierValue >= 0) "+" else ""


    val pointsAboveBase = boughtValue - 8

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)


        Text(
            text = "$modSign$modifierValue",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalValue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = "8 + $pointsAboveBase + $totalBonus",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(2.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onValueChange(-1) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onValueChange(1) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\StatsStep.kt
