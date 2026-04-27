package com.aplivit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aplivit.core.domain.usecase.NavigationUseCase
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
import com.aplivit.presentation.screen.exercise.LetterTracingScreen
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
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private data class HomeRoute(val completed: Boolean = false)

@Serializable
private data class LevelRoute(val levelId: Int)

@Serializable
private data class GameRoute(val levelId: Int)

@Serializable
private data class RecapRoute(val nextLevelId: Int)

private const val ROUTE_TOUCH_TEST = "touch_test"
private const val ROUTE_TOUCH_SYLLABLE_TEST = "touch_syllable_test"
private const val ROUTE_TOUCH_ORDER_TEST = "touch_order_test"
private const val ROUTE_TOUCH_ORDER_WORDS_TEST = "touch_order_words_test"
private const val ROUTE_LINK_TEST = "link_test"
private const val ROUTE_DRAG_TEST = "drag_test"
private const val ROUTE_VOCALIZE_TEST = "vocalize_test"
private const val ROUTE_LETTER_TRACING_TEST = "letter_tracing_test"

/** Mostrar RecapScreen cada N niveles completados. Cambiar este valor para ajustar la frecuencia. */
private const val RECAP_EVERY_N_LEVELS = 3

/** Total de niveles disponibles en los archivos JSON. Actualizar si se agregan niveles. */
private const val TOTAL_LEVELS = 15

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navUseCase: NavigationUseCase = koinInject()

    NavHost(navController = navController, startDestination = HomeRoute()) {
        composable<HomeRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HomeRoute>()
            HomeScreen(
                onLevelClick = { levelId ->
                    navController.navigate(GameRoute(levelId)) {
                        popUpTo<HomeRoute> { inclusive = true }
                    }
                },
                completed = route.completed
            )
        }
        composable<LevelRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<LevelRoute>()
            LevelScreen(
                levelId = route.levelId,
                onStartGames = { navController.navigate(GameRoute(route.levelId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<GameRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GameRoute>()
            val levelId = route.levelId
            GameScreen(
                levelId = levelId,
                onCompleted = { nextLevelId ->
                    if (nextLevelId > TOTAL_LEVELS) {
                        navController.navigate(HomeRoute(completed = true)) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        val completedLevelId = nextLevelId - 1
                        val showRecap = completedLevelId % RECAP_EVERY_N_LEVELS == 0
                        if (showRecap) {
                            navController.navigate(RecapRoute(nextLevelId)) {
                                popUpTo<GameRoute> { inclusive = true }
                            }
                        } else {
                            navController.navigate(GameRoute(nextLevelId)) {
                                popUpTo<GameRoute> { inclusive = true }
                            }
                        }
                    }
                },
                onBackNavigate = {
                    val (prevLevel, _) = navUseCase.goBack(levelId, 1)
                    if (prevLevel < levelId) {
                        navController.navigate(GameRoute(prevLevel)) {
                            popUpTo<GameRoute> { inclusive = true }
                        }
                    } else {
                        navController.navigate(HomeRoute()) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable<RecapRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RecapRoute>()
            RecapScreen(
                onBackClick = { navController.popBackStack() },
                onForwardClick = {
                    if (route.nextLevelId > TOTAL_LEVELS) {
                        navController.navigate(HomeRoute(completed = true)) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(GameRoute(route.nextLevelId)) {
                            popUpTo<RecapRoute> { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(ROUTE_VOCALIZE_TEST) {
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
        composable(ROUTE_LETTER_TRACING_TEST) {
            LetterTracingScreen(
                letter = "A",
                onBackClick = { navController.popBackStack() },
                onForwardClick = { navController.popBackStack() }
            )
        }
    }
}
