// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\navigation\Screen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

sealed class Screen(val route: String) {

    data object CharacterList : Screen("character_list")


    data object CharacterCreator : Screen("character_creator?draftId={draftId}") {
        fun createRoute(draftId: Long = 0L) = "character_creator?draftId=$draftId"
    }


    data object CharacterSheet : Screen("character_sheet/{characterId}") {
        fun createRoute(characterId: Long) = "character_sheet/$characterId"
    }


    data object LevelUp : Screen("level_up/{characterId}") {
        fun createRoute(characterId: Long) = "level_up/$characterId"
    }

    data object ShapeSelector : Screen("shape_selector/{characterId}") {
        fun createRoute(characterId: Long) = "shape_selector/$characterId"
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\navigation\Screen.kt
