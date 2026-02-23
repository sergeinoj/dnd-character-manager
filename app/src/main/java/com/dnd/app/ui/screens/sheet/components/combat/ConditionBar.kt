package com.dnd.app.ui.screens.sheet.components.combat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CONDITION_LABELS = mapOf(
    "Blinded" to "\u041e\u0441\u043b\u0435\u043f\u043b\u0435\u043d",
    "Charmed" to "\u041e\u0447\u0430\u0440\u043e\u0432\u0430\u043d",
    "Deafened" to "\u041e\u0433\u043b\u043e\u0445\u043b\u0435\u043d",
    "Exhaustion" to "\u0418\u0441\u0442\u043e\u0449\u0435\u043d\u0438\u0435",
    "Frightened" to "\u0418\u0441\u043f\u0443\u0433\u0430\u043d",
    "Grappled" to "\u0421\u0445\u0432\u0430\u0447\u0435\u043d",
    "Incapacitated" to "\u041d\u0435\u0441\u043f\u043e\u0441\u043e\u0431\u0435\u043d",
    "Invisible" to "\u041d\u0435\u0432\u0438\u0434\u0438\u043c",
    "Paralyzed" to "\u041f\u0430\u0440\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d",
    "Petrified" to "\u041e\u043a\u0430\u043c\u0435\u043d\u0435\u043b",
    "Poisoned" to "\u041e\u0442\u0440\u0430\u0432\u043b\u0435\u043d",
    "Prone" to "\u0421\u0431\u0438\u0442 \u0441 \u043d\u043e\u0433",
    "Restrained" to "\u0421\u0432\u044f\u0437\u0430\u043d",
    "Stunned" to "\u041e\u0433\u043b\u0443\u0448\u0435\u043d",
    "Unconscious" to "\u0411\u0435\u0441\u0441\u043e\u0437\u043d\u0430\u0442\u0435\u043b\u0435\u043d",
    "Diseased" to "\u0411\u043e\u043b\u0435\u043d",
    "Cursed" to "\u041f\u0440\u043e\u043a\u043b\u044f\u0442"
)

private val SEVERITY_COLORS = mapOf(
    "Paralyzed" to Color(0xFFB71C1C),
    "Petrified" to Color(0xFFB71C1C),
    "Stunned" to Color(0xFFB71C1C),
    "Unconscious" to Color(0xFFB71C1C),
    "Incapacitated" to Color(0xFFE65100),
    "Exhaustion" to Color(0xFFE65100),
    "Poisoned" to Color(0xFF7B1FA2),
    "Blinded" to Color(0xFF7B1FA2),
    "Charmed" to Color(0xFF1565C0),
    "Frightened" to Color(0xFF1565C0),
    "Prone" to Color(0xFF388E3C),
    "Restrained" to Color(0xFF388E3C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionBar(
    activeConditions: Set<String>,
    exhaustionLevel: Int = 0,
    isConcentrating: Boolean = false,
    onAddClick: () -> Unit,
    onRemoveCondition: (String) -> Unit,
    onDecreaseExhaustion: () -> Unit = {},
    onResetConcentration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val iconAlpha = if (isDark) 0.7f else 0.5f
    
    val exhaustionColor = if (isDark) Color(0xFFB71C1C) else Color(0xFFFFCDD2)
    
    val hasExhaustion = exhaustionLevel > 0
    val totalItems = activeConditions.size + (if (hasExhaustion) 1 else 0) + (if (isConcentrating) 1 else 0)

    if (totalItems == 0) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = onAddClick,
                label = { 
                    Text(
                        "+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                modifier = Modifier.height(26.dp)
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isConcentrating) {
            val concentrationColor = if (isDark) Color(0xFF6D4C00) else Color(0xFFFFE082)
            InputChip(
                selected = true,
                onClick = onResetConcentration,
                label = { 
                    Text(
                        "\u25cf \u041a\u043e\u043d\u0446\u0435\u043d\u0442\u0440\u0430\u0446\u0438\u044f",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ) 
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = concentrationColor,
                    labelColor = textColor
                ),
                modifier = Modifier.height(26.dp),
                trailingIcon = {
                    Text(
                        "\u2715",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = iconAlpha)
                    )
                }
            )
        }
        if (hasExhaustion) {
            InputChip(
                selected = true,
                onClick = onDecreaseExhaustion,
                label = { 
                    Text(
                        "\u0418\u0441\u0442\u043e\u0449\u0435\u043d\u0438\u0435 $exhaustionLevel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ) 
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = exhaustionColor,
                    labelColor = textColor
                ),
                modifier = Modifier.height(26.dp),
                trailingIcon = {
                    Text(
                        "\u2715",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = iconAlpha)
                    )
                }
            )
        }
        activeConditions.forEach { condition ->
            val label = CONDITION_LABELS[condition] ?: condition
            val darkColor = SEVERITY_COLORS[condition] ?: Color(0xFF546E7A)
            val chipColor = if (isDark) darkColor else darkColor.copy(alpha = 0.3f)
            InputChip(
                selected = true,
                onClick = { onRemoveCondition(condition) },
                label = { 
                    Text(
                        label, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ) 
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = chipColor,
                    labelColor = textColor
                ),
                modifier = Modifier.height(26.dp),
                trailingIcon = {
                    Text(
                        "\u2715",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = iconAlpha)
                    )
                }
            )
        }
        AssistChip(
            onClick = onAddClick,
            label = { Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.height(26.dp)
        )
    }
}
