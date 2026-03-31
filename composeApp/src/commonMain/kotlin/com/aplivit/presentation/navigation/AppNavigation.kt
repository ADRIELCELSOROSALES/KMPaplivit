package com.aplivit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aplivit.presentation.screen.game.GameScreen
import com.aplivit.presentation.screen.home.HomeScreen
import com.aplivit.presentation.screen.level.LevelScreen
import com.aplivit.presentation.screen.settings.SettingsScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_HOME_PATTERN = "home?completed={completed}"
private const val ROUTE_LEVEL = "level/{levelId}"
private const val ROUTE_GAME = "game/{levelId}"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(
            route = ROUTE_HOME_PATTERN,
            arguments = listOf(
                navArgument("completed") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val completed = backStackEntry.arguments?.getBoolean("completed") ?: false
            HomeScreen(
                onLevelClick = { levelId -> navController.navigate("level/$levelId") },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                completed = completed
            )
        }
        composable(ROUTE_LEVEL) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId")?.toIntOrNull() ?: 1
            LevelScreen(
                levelId = levelId,
                onStartGames = { navController.navigate("game/$levelId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTE_GAME) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId")?.toIntOrNull() ?: 1
            GameScreen(
                levelId = levelId,
                onCompleted = {
                    navController.navigate("home?completed=true") {
                        popUpTo(ROUTE_HOME_PATTERN) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_HOME_PATTERN) { inclusive = true }
                    }
                }
            )
        }
    }
}
