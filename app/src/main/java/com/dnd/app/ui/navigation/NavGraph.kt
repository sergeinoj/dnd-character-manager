// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\navigation\NavGraph.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dnd.app.ui.screens.character_creator.CharacterCreatorScreen
import com.dnd.app.ui.screens.character_creator.level_up.LevelUpScreen
import com.dnd.app.ui.screens.character_list.CharacterListScreen
import com.dnd.app.ui.screens.shape.ShapeSelectorScreen
import com.dnd.app.ui.screens.sheet.CharacterSheetScreen

@Composable
fun DndNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route,
        modifier = modifier
    ) {
        composable(route = Screen.CharacterList.route) {
            CharacterListScreen(
                onNavigateToCreate = { navController.navigate(Screen.CharacterCreator.createRoute(0L)) },
                onNavigateToSheet = { id -> navController.navigate(Screen.CharacterSheet.createRoute(id)) }
            )
        }

        composable(
            route = Screen.CharacterCreator.route,
            arguments = listOf(navArgument("draftId") { type = NavType.LongType; defaultValue = 0L })
        ) {
            CharacterCreatorScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.CharacterSheet.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val charId = backStackEntry.arguments?.getLong("characterId") ?: 0L
            CharacterSheetScreen(
                navigateUp = { navController.navigateUp() },
                onEditCharacter = { navController.navigate(Screen.CharacterCreator.createRoute(charId)) },
                onLevelUp = { navController.navigate(Screen.LevelUp.createRoute(charId)) },
                onOpenShapeSelector = { navController.navigate(Screen.ShapeSelector.createRoute(charId)) }
            )
        }

        composable(
            route = Screen.LevelUp.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) {
            LevelUpScreen(
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.ShapeSelector.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) {
            ShapeSelectorScreen(
                navigateUp = { navController.navigateUp() }
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\navigation\NavGraph.kt
