package com.treasure.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.treasure.ui.detail.DetailRoute
import com.treasure.ui.edit.EditRoute
import com.treasure.ui.main.MainScreen

private val SlideTween = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 300)

@Composable
fun TreasureNavHost() {
    val nav: NavHostController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.Main,
            // Push: 新页从右滑入；旧页向左滑出。
            // Pop: 上一页从左滑回；当前页向右滑出。
            enterTransition  = { slideIntoContainer(SlideDirection.Start, SlideTween) },
            exitTransition   = { slideOutOfContainer(SlideDirection.Start, SlideTween) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End, SlideTween) },
            popExitTransition  = { slideOutOfContainer(SlideDirection.End, SlideTween) },
        ) {
            composable(Routes.Main) {
                MainScreen(
                    onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                )
            }

            composable(
                route = Routes.DetailPattern,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("itemId").orEmpty()
                DetailRoute(
                    itemId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { idToEdit -> nav.navigate(Routes.edit(idToEdit)) },
                )
            }

            composable(
                route = Routes.EditPattern,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("itemId").orEmpty()
                EditRoute(
                    itemId = id,
                    onDone = { nav.popBackStack() },
                )
            }
        }
    }
}
