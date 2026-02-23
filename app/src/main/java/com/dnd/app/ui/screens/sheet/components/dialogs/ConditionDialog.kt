package com.dnd.app.ui.screens.sheet.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DND_CONDITIONS = listOf(
    "Blinded" to "\u041e\u0441\u043b\u0435\u043f\u043b\u0435\u043d",
    "Charmed" to "\u041e\u0447\u0430\u0440\u043e\u0432\u0430\u043d",
    "Deafened" to "\u041e\u0433\u043b\u043e\u0445\u043b\u0435\u043d",
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

@Composable
fun ConditionDialog(
    activeConditions: Set<String>,
    exhaustionLevel: Int = 0,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onIncreaseExhaustion: () -> Unit = {},
    onDecreaseExhaustion: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u044f", fontWeight = FontWeight.Bold) 
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "\u0418\u0441\u0442\u043e\u0449\u0435\u043d\u0438\u0435",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDecreaseExhaustion,
                            enabled = exhaustionLevel > 0
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            " $exhaustionLevel ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (exhaustionLevel > 0) Color(0xFFC62828) else Color.Unspecified
                        )
                        IconButton(
                            onClick = onIncreaseExhaustion,
                            enabled = exhaustionLevel < 6
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
                items(DND_CONDITIONS) { (eng, rus) ->
                    val isActive = eng in activeConditions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isActive) onRemove(eng) else onAdd(eng)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isActive,
                            onCheckedChange = { checked ->
                                if (checked) onAdd(eng) else onRemove(eng)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFC62828),
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = rus,
                            fontSize = 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color(0xFFC62828) else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("\u0413\u043e\u0442\u043e\u0432\u043e")
            }
        }
    )
}
