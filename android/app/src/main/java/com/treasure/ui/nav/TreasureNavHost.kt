package com.treasure.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.treasure.core.domain.Category
import com.treasure.ui.components.ControlIsland
import com.treasure.ui.components.IslandTab
import com.treasure.ui.detail.DetailRoute
import com.treasure.ui.grid.GridRoute
import com.treasure.ui.portal.PortalRoute
import com.treasure.ui.stubs.AddStubScreen
import com.treasure.ui.stubs.SettingsStubScreen

private val SlideTween = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 300)

@Composable
fun TreasureNavHost() {
    val nav: NavHostController = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.Portal,
            // Push: new page enters from the right; old slides off to the left.
            // Pop: previous page re-enters from the left; top slides off to the right.
            enterTransition  = { slideIntoContainer(SlideDirection.Start, SlideTween) },
            exitTransition   = { slideOutOfContainer(SlideDirection.Start, SlideTween) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End, SlideTween) },
            popExitTransition  = { slideOutOfContainer(SlideDirection.End, SlideTween) },
        ) {
            composable(Routes.Portal) {
                PortalRoute(
                    onEnterCategory = { cat -> nav.navigate(Routes.grid(cat)) },
                    onOpenItem = { id -> nav.navigate(Routes.detail(id)) },
                )
            }

            composable(
                route = Routes.GridPattern,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId") ?: Category.PHOTO.id
                GridRoute(
                    initialCategoryId = categoryId,
                    onBack = { nav.popBackStack() },
                    onOpenItem = { id -> nav.navigate(Routes.detail(id)) },
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
                )
            }

            composable(Routes.Add) { AddStubScreen() }
            composable(Routes.Settings) { SettingsStubScreen() }
        }

        val islandTab = islandTabFor(currentRoute)
        if (islandTab != null) {
            ControlIsland(
                selected = islandTab,
                onSelect = { picked ->
                    val target = when (picked) {
                        IslandTab.Portal -> Routes.Portal
                        IslandTab.Grid ->
                            if (currentRoute?.startsWith("grid/") == true) currentRoute
                            else Routes.grid(Category.PHOTO)
                        IslandTab.Add -> Routes.Add
                        IslandTab.Settings -> Routes.Settings
                    }
                    if (target != currentRoute) {
                        nav.navigate(target) {
                            popUpTo(Routes.Portal) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
            )
        }
    }
}

private fun islandTabFor(route: String?): IslandTab? = when {
    route == Routes.Portal -> IslandTab.Portal
    route?.startsWith("grid/") == true -> IslandTab.Grid
    route == Routes.Add -> IslandTab.Add
    route == Routes.Settings -> IslandTab.Settings
    route?.startsWith("detail/") == true -> null  // hide on Detail per visual-language.md
    else -> null
}
