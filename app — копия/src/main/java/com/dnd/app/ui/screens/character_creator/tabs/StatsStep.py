// Имя файла: ui/screens/character_creator/tabs/StatsStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.WizardState
import com.dnd.app.ui.screens.character_creator.components.WizardSection
import com.dnd.app.ui.theme.DndPrimary
import com.dnd.app.ui.theme.DndSurface

@Composable
fun StatsStep(
    state: WizardState,
    viewModel: CharacterCreatorViewModel
    // ИСПРАВЛЕНИЕ: Удален неиспользуемый параметр calculator
) {
    val draft = state.draft
    val points = draft.pointsRemaining

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (points == 0) DndPrimary else Color(0xFFB00020), RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Очков осталось", color = Color.LightGray, fontSize = 12.sp)
                Text(
                    "$points / ${DndRules.MAX_POINTS}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        WizardSection("Распределение (Point Buy)") {
            StatInputBinder("Сила", "STR", draft.baseStr, viewModel)
            StatInputBinder("Ловкость", "DEX", draft.baseDex, viewModel)
            StatInputBinder("Телосложение", "CON", draft.baseCon, viewModel)
            StatInputBinder("Интеллект", "INT", draft.baseInt, viewModel)
            StatInputBinder("Мудрость", "WIS", draft.baseWis, viewModel)
            StatInputBinder("Харизма", "CHA", draft.baseCha, viewModel)
        }
    }
}

@Composable
private fun StatInputBinder(
    uiName: String,
    statCode: String,
    value: Int,
    viewModel: CharacterCreatorViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DndSurface, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(uiName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.updateBaseStat(statCode, -1) }) {
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("${DndRules.getPointCost(value)} очк.", fontSize = 10.sp, color = Color.Gray)
            }

            IconButton(onClick = { viewModel.updateBaseStat(statCode, 1) }) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_creator/tabs/StatsStep.kt