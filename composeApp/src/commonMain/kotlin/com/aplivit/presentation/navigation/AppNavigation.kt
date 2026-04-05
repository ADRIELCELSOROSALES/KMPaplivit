package com.aplivit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aplivit.core.domain.model.LinkExercise
import com.aplivit.core.domain.model.LinkItem
import com.aplivit.core.domain.model.LinkPair
import com.aplivit.core.domain.model.LinkType
import com.aplivit.core.domain.model.SentenceExercise
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.domain.model.TouchType
import com.aplivit.core.domain.model.DragExercise
import com.aplivit.core.domain.model.DragType
import com.aplivit.core.domain.model.VocalizeExercise
import com.aplivit.core.domain.model.VocalizeType
import com.aplivit.presentation.screen.exercise.DragExerciseScreen
import com.aplivit.presentation.screen.exercise.VocalizeExerciseScreen
import com.aplivit.presentation.screen.exercise.LinkExerciseScreen
import com.aplivit.presentation.screen.exercise.TouchOrderSyllablesScreen
import com.aplivit.presentation.screen.exercise.TouchOrderWordsScreen
import com.aplivit.presentation.screen.exercise.TouchSimilarScreen
import com.aplivit.presentation.screen.exercise.TouchSyllableInWordScreen
import com.aplivit.presentation.screen.game.GameScreen
import com.aplivit.presentation.screen.home.HomeScreen
import com.aplivit.presentation.screen.level.LevelScreen
import com.aplivit.presentation.screen.recap.RecapScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_HOME_PATTERN = "home?completed={completed}"
private const val ROUTE_LEVEL = "level/{levelId}"
private const val ROUTE_GAME = "game/{levelId}"
private const val ROUTE_RECAP = "recap/{nextLevelId}"
private const val ROUTE_TOUCH_TEST = "touch_test"
private const val ROUTE_TOUCH_SYLLABLE_TEST = "touch_syllable_test"
private const val ROUTE_TOUCH_ORDER_TEST = "touch_order_test"
private const val ROUTE_TOUCH_ORDER_WORDS_TEST = "touch_order_words_test"
private const val ROUTE_LINK_TEST = "link_test"
private const val ROUTE_DRAG_TEST = "drag_test"
private const val ROUTE_VOCALIZE_TEST = "vocalize_test"

/** Mostrar RecapScreen cada N niveles completados. Cambiar este valor para ajustar la frecuencia. */
private const val RECAP_EVERY_N_LEVELS = 3

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
                    val completedLevelId = nextLevelId - 1
                    val showRecap = completedLevelId % RECAP_EVERY_N_LEVELS == 0
                    if (showRecap) {
                        navController.navigate("recap/$nextLevelId") {
                            popUpTo(ROUTE_GAME) { inclusive = true }
                        }
                    } else {
                        navController.navigate("game/$nextLevelId") {
                            popUpTo(ROUTE_GAME) { inclusive = true }
                        }
                    }
                },
                onBackNavigate = { navController.popBackStack() }
            )
        }
        composable(
            route = ROUTE_RECAP,
            arguments = listOf(navArgument("nextLevelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val nextLevelId = backStackEntry.arguments?.getInt("nextLevelId") ?: 1
            RecapScreen(
                onBackClick = { navController.popBackStack() },
                onForwardClick = {
                    navController.navigate("game/$nextLevelId") {
                        popUpTo(ROUTE_RECAP) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_VOCALIZE_TEST) {
            // Cambiar type/content para probar SYLLABLE, WORD o SENTENCE
            VocalizeExerciseScreen(
                exercise = VocalizeExercise(
                    id = 8,
                    type = VocalizeType.SENTENCE,
                    content = "El gato come leche"
                ),
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
        composable(ROUTE_DRAG_TEST) {
            // Tarea 8: sílabas → palabra
            val sampleExercise = DragExercise(
                id = 6,
                type = DragType.SYLLABLES_TO_WORD,
                items = listOf("MA", "ME", "SA"),
                targetSlots = 3
            )
            DragExerciseScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
        composable("drag_sentence_test") {
            // Tarea 9: palabras → oración
            val sampleExercise = DragExercise(
                id = 7,
                type = DragType.WORDS_TO_SENTENCE,
                items = listOf("El", "perro", "corre", "rápido"),
                targetSlots = 4
            )
            DragExerciseScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
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
        composable(ROUTE_TOUCH_ORDER_TEST) {
            // Ejemplo: palabra "MAMÁ", sílabas desordenadas ["MÁ", "MA"], orden correcto → [1, 0]
            val sampleExercise = TouchExercise(
                id = 3,
                type = TouchType.ORDER_SYLLABLES,
                target = "MAMÁ",
                options = listOf("MÁ", "MA"),
                correctIndices = listOf(1, 0)
            )
            TouchOrderSyllablesScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
        composable(ROUTE_TOUCH_ORDER_WORDS_TEST) {
            // Ejemplo: "El gato come" → mostrado como ["come", "El", "gato"] (shuffledIndices=[2,0,1])
            val sampleExercise = SentenceExercise(
                id = 4,
                words = listOf("El", "gato", "come"),
                shuffledIndices = listOf(2, 0, 1)
            )
            TouchOrderWordsScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
        composable(ROUTE_LINK_TEST) {
            // Ejemplo SAME_FORMS: vincular sílabas iguales
            val sampleExercise = LinkExercise(
                id = 5,
                type = LinkType.SAME_FORMS,
                leftItems = listOf(
                    LinkItem(text = "MA"),
                    LinkItem(text = "PA"),
                    LinkItem(text = "SA")
                ),
                rightItems = listOf(
                    LinkItem(text = "SA"),
                    LinkItem(text = "MA"),
                    LinkItem(text = "PA")
                ),
                correctPairs = listOf(
                    LinkPair(left = 0, right = 1),
                    LinkPair(left = 1, right = 2),
                    LinkPair(left = 2, right = 0)
                )
            )
            LinkExerciseScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
        composable(ROUTE_TOUCH_SYLLABLE_TEST) {
            val sampleExercise = TouchExercise(
                id = 2,
                type = TouchType.SYLLABLE_IN_WORD,
                target = "MA",
                options = listOf("MA", "MA"),
                correctIndices = listOf(0, 1)
            )
            TouchSyllableInWordScreen(
                exercise = sampleExercise,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
    }
}
