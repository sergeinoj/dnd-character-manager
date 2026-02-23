// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\DndApp.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dnd.app.ui.navigation.DndNavGraph
import com.dnd.app.ui.theme.DndTheme

@Composable
fun DndApp() {
    DndTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            DndNavGraph(navController = navController)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\DndApp.kt