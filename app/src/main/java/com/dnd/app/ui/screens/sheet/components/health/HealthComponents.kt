package com.dnd.app.ui.screens.sheet.components.health

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.snapshot.DeathSavesState
import com.dnd.app.ui.screens.sheet.components.dialogs.HpModifierDialog
import com.dnd.app.ui.screens.sheet.components.dialogs.TempHpDialog
import com.dnd.app.ui.theme.DndBonusGreen
import com.dnd.app.ui.theme.DndMalusRed

@Composable
fun HealthButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.background(color, RoundedCornerShape(4.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun QuickHpButton(text: String, color: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(46.dp).clickable { onClick() }, color = color, shape = RoundedCornerShape(4.dp), shadowElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) { Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
    }
}

@Composable
fun DeathSavesInteractiveRow(state: DeathSavesState, onSaveClick: (isSuccess: Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF212121), RoundedCornerShape(8.dp)).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("СПАСБРОСКИ ОТ СМЕРТИ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DeathSaveCounter("УСПЕХ", DndBonusGreen, state.successes) { onSaveClick(true) }
            DeathSaveCounter("ПРОВАЛ", DndMalusRed, state.failures) { onSaveClick(false) }
        }
    }
}

@Composable
private fun DeathSaveCounter(label: String, color: Color, count: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.padding(top = 6.dp)) { repeat(3) { i -> Box(modifier = Modifier.padding(horizontal = 3.dp).size(18.dp).border(1.dp, color, CircleShape).background(if (i < count) color else Color.Transparent, CircleShape)) } }
    }
}

@Composable
fun HealthWidget(
    current: Int, max: Int, temp: Int, onDamage: (Int) -> Unit, onHeal: (Int) -> Unit, onSetTempHp: (Int) -> Unit,
    isTransformed: Boolean = false, transformationHp: Int = 0, transformationMaxHp: Int = 0
) {
    val isUnconscious = current <= 0
    val shownCurrent = if (isTransformed) transformationHp else current
    val shownMax = if (isTransformed) transformationMaxHp.coerceAtLeast(1) else max.coerceAtLeast(1)
    val shownProgress = (shownCurrent.toFloat() / shownMax.toFloat()).coerceIn(0f, 1f)
    val hpColor = if (isTransformed) Color(0xFF22C55E) else if (isUnconscious) Color(0xFFB71C1C) else Color(0xFF90CAF9)
    var showHpDialog by remember { mutableStateOf(false) }
    var showTempHpDialog by remember { mutableStateOf(false) }
    if (showHpDialog) HpModifierDialog("Изменение здоровья", { showHpDialog = false }, { onDamage(it); showHpDialog = false }, { onHeal(it); showHpDialog = false })
    if (showTempHpDialog) TempHpDialog("Временные хиты", { showTempHpDialog = false }, { onSetTempHp(it); showTempHpDialog = false })
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Column { QuickHpButton("-10", DndMalusRed) { onDamage(10) }; Spacer(Modifier.height(8.dp)); QuickHpButton("-1", DndMalusRed.copy(alpha = 0.7f)) { onDamage(1) } }
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.size(124.dp).background(if (isUnconscious) Color(0xFF420000) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(2.dp, if (isUnconscious) Color.Red else Color.Gray, RoundedCornerShape(8.dp)).clickable { showHpDialog = true }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isTransformed) "ЗВЕРЬ" else if (isUnconscious) "СМЕРТЬ" else "ХИТЫ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isUnconscious) Color.White else MaterialTheme.colorScheme.onSurface)
                Text("$shownCurrent", fontSize = 38.sp, fontWeight = FontWeight.Black, color = if (isUnconscious) Color.White else MaterialTheme.colorScheme.onSurface)
                Text("/ $shownMax", fontSize = 14.sp, color = if (isUnconscious) Color.LightGray else Color.Gray)
                LinearProgressIndicator(progress = shownProgress, color = hpColor, trackColor = Color(0xFFE5E7EB), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp))
                if (isTransformed) Text("Тело героя: $current/$max", fontSize = 10.sp, color = Color.Gray)
                Text(if (temp > 0) "+$temp ВРЕМ." else "ВРЕМ. ХИТЫ", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { showTempHpDialog = true })
            }
        }
        Spacer(Modifier.width(16.dp))
        Column { QuickHpButton("+10", DndBonusGreen) { onHeal(10) }; Spacer(Modifier.height(8.dp)); QuickHpButton("+1", DndBonusGreen.copy(alpha = 0.7f)) { onHeal(1) } }
    }
}

