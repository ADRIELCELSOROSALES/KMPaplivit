package com.aplivit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.domain.model.TouchType
import com.aplivit.presentation.screen.exercise.TouchSimilarScreen
import com.aplivit.presentation.screen.game.GameScreen
import com.aplivit.presentation.screen.home.HomeScreen
import com.aplivit.presentation.screen.level.LevelScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_HOME_PATTERN = "home?completed={completed}"
private const val ROUTE_LEVEL = "level/{levelId}"
private const val ROUTE_GAME = "game/{levelId}"
private const val ROUTE_TOUCH_TEST = "touch_test"

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
                onLevelClick = { levelId ->
                    navController.navigate("game/$levelId") {
                        popUpTo(ROUTE_HOME_PATTERN) { inclusive = true }
                    }
                },
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
                onCompleted = { nextLevelId ->
                    navController.navigate("game/$nextLevelId") {
                        popUpTo(ROUTE_GAME) { inclusive = true }
                    }
                },
                onBackNavigate = { navController.popBackStack() }
            )
        }
        composable(ROUTE_TOUCH_TEST) {
            val sampleExercise = TouchExercise(
                id = 1,
                type = TouchType.SIMILAR_FORMS,
                target = "MA",
                options = listOf("MA", "ME", "MA", "MI", "MA"),
                correctIndices = listOf(0, 2, 4)
            )
            TouchSimilarScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
    }
}
