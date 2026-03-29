package com.aplivit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aplivit.presentation.screen.game.GameScreen
import com.aplivit.presentation.screen.home.HomeScreen
import com.aplivit.presentation.screen.level.LevelScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_LEVEL = "level/{levelId}"
private const val ROUTE_GAME = "game/{levelId}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onLevelClick = { levelId ->
                    navController.navigate("level/$levelId")
                }
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
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                }
            )
        }
    }
}
