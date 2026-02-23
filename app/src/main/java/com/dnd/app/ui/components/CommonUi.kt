// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\components\CommonUi.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.dnd.app.ui.theme.DndPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndTopBar(
    title: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White),
        navigationIcon = { if (canNavigateBack) IconButton(onClick = navigateUp) { Icon(Icons.Filled.ArrowBack, "Back") } },
        actions = { actions() }
    )
}

@Composable
fun DndActionTopBar(
    title: String,
    onBack: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    secondaryActionIcon: @Composable (() -> Unit)? = null,
    isActionEnabled: Boolean = true,
    level: Int = 0,
    onLevelChange: ((Int) -> Unit)? = null,
    onDebugClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(DndPrimary).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(4.dp)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.Black, modifier = Modifier.size(32.dp))
        }


        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )


        if (onDebugClick != null) {
            IconButton(onClick = onDebugClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Debug Inspector",
                    tint = Color.White
                )
            }
        }


        if (level > 0 && onLevelChange != null) {
            LevelSelector(level, onLevelChange)
            Spacer(modifier = Modifier.width(8.dp))
        }


        if (actionIcon != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isActionEnabled) Color.White else Color.Gray, RoundedCornerShape(4.dp))
                    .then(
                        if (onActionClick != null) {
                            Modifier.clickable(enabled = isActionEnabled, onClick = onActionClick)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                actionIcon()
            }
        }

        if (secondaryActionIcon != null && onSecondaryActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .clickable(onClick = onSecondaryActionClick),
                contentAlignment = Alignment.Center
            ) {
                secondaryActionIcon()
            }
        }
    }
}

@Composable
fun LevelSelector(level: Int, onLevelChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable { expanded = true }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ур. $level", fontWeight = FontWeight.Bold, color = Color.Black)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (i in 1..20) {
                DropdownMenuItem(
                    text = { Text("Уровень $i") },
                    onClick = { onLevelChange(i); expanded = false }
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\components\CommonUi.kt
