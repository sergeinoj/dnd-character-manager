// Имя файла: MainActivity.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dnd.app.ui.DndApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Возвращаем приложение к жизни!
        setContent {
            DndApp()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: MainActivity.kt