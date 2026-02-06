// Имя файла: ui/navigation/NavGraph.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// DndCalculator больше не нужен здесь
import com.dnd.app.ui.screens.character_creator.CharacterCreatorScreen
import com.dnd.app.ui.screens.character_list.CharacterListScreen
import com.dnd.app.ui.screens.character_sheet.CharacterSheetScreen

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
                onNavigateToCreate = { navController.navigate(Screen.CharacterCreator.route) },
                onNavigateToSheet = { id -> navController.navigate(Screen.CharacterSheet.createRoute(id)) }
            )
        }

        composable(route = Screen.CharacterCreator.route) {
            CharacterCreatorScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.CharacterSheet.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) {
            CharacterSheetScreen(
                navigateUp = { navController.navigateUp() }
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/navigation/NavGraph.kt