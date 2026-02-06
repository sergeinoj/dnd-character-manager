// Имя файла: ui/navigation/Screen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

sealed class Screen(val route: String) {
    // Главный экран: Список персонажей
    data object CharacterList : Screen("character_list")

    // Экран создания: Мастер
    data object CharacterCreator : Screen("character_creator")

    // Экран персонажа: Передаем ID
    data object CharacterSheet : Screen("character_sheet/{characterId}") {
        fun createRoute(characterId: Long) = "character_sheet/$characterId"
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/navigation/Screen.kt