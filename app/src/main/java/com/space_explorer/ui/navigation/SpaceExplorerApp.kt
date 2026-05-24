package com.space_explorer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.space_explorer.R
import com.space_explorer.ui.screens.DetailScreen
import com.space_explorer.ui.screens.FavoritesScreen
import com.space_explorer.ui.screens.HomeScreen

@Composable
fun SpaceExplorerApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in TOP_LEVEL_ROUTES) {
                NavigationBar {
                    bottomBarItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == item.destination.route,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = label) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("tab_${item.destination.route}"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HomeDestination.route) {
                HomeScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onAstronomyClick = { astronomy ->
                        navController.navigate(DetailDestination.build(astronomy.id))
                    },
                )
            }
            composable(FavoritesDestination.route) {
                FavoritesScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onAstronomyClick = { astronomy ->
                        navController.navigate(DetailDestination.build(astronomy.id))
                    },
                )
            }
            composable(
                route = DetailDestination.route,
                arguments = listOf(
                    navArgument(DetailDestination.ARG_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                DetailScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private data class BottomBarItem(
    val destination: Destination,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val bottomBarItems = listOf(
    BottomBarItem(HomeDestination, R.string.nav_tab_explore, Icons.Outlined.Public),
    BottomBarItem(FavoritesDestination, R.string.nav_tab_favorites, Icons.Outlined.Star),
)

private val TOP_LEVEL_ROUTES = setOf(HomeDestination.route, FavoritesDestination.route)
