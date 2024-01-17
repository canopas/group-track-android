package com.canopas.catchme.ui.flow.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.R
import com.canopas.catchme.ui.flow.home.activity.ActivityScreen
import com.canopas.catchme.ui.flow.home.map.MapScreen
import com.canopas.catchme.ui.flow.home.places.PlacesScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<HomeScreenViewModel>()

    AppNavigator(navController = navController, viewModel.navActions)

    Scaffold(
        content = {
            Box(
                modifier = Modifier
                    .padding(it)
            ) {
                HomeScreenContent(navController)
            }
        },
        bottomBar = {
            HomeBottomBar(navController)
        }
    )
}

@Composable
fun HomeScreenContent(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.map.path
    ) {
        composable(AppDestinations.map.path) {
            MapScreen()
        }

        composable(AppDestinations.places.path) {
            PlacesScreen()
        }

        composable(AppDestinations.activity.path) {
            ActivityScreen()
        }
    }
}

@Composable
fun HomeBottomBar(navController: NavHostController) {
    fun navigateTo(route: String) {
        navController.navigate(route) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationBar(
        contentColor = AppTheme.colorScheme.primary,
        containerColor = AppTheme.colorScheme.containerNormalOnSurface
    ) {
        NavItem(
            HomeTab.Main,
            true
        ) {
            navigateTo(AppDestinations.map.path)
        }

        NavItem(
            HomeTab.Places,
            false
        ) {
            navigateTo(AppDestinations.places.path)
        }

        NavItem(
            HomeTab.Activities,
            false
        ) {
            navigateTo(AppDestinations.activity.path)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    screen: HomeTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (screen.route) {
        HomeTab.Main.route -> stringResource(R.string.home_tab_label_home)
        HomeTab.Places.route -> stringResource(R.string.home_tab_label_places)
        HomeTab.Activities.route -> stringResource(R.string.home_tab_label_activities)
        else -> stringResource(R.string.home_tab_label_home)
    }

    NavigationBarItem(
        icon = {
            Icon(
                painter = painterResource(id = if (isSelected) screen.resourceIdFilled else screen.resourceIdLine),
                null,
                modifier = Modifier.size(24.dp)
            )
        },
        selected = isSelected,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = AppTheme.colorScheme.containerNormalOnSurface,
            selectedIconColor = AppTheme.colorScheme.primary,
            selectedTextColor = AppTheme.colorScheme.primary,
            unselectedIconColor = AppTheme.colorScheme.textDisabled,
            unselectedTextColor = AppTheme.colorScheme.textDisabled
        ),
        alwaysShowLabel = true,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTheme.appTypography.label3,
                fontSize = 10.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

sealed class HomeTab(
    val route: String,
    @DrawableRes val resourceIdLine: Int,
    @DrawableRes val resourceIdFilled: Int
) {
    object Main : HomeTab(
        "Main",
        R.drawable.ic_tab_home_outlined,
        R.drawable.ic_tab_home_filled
    )

    object Places : HomeTab(
        "Places",
        R.drawable.ic_tab_places_outlined,
        R.drawable.ic_tab_places_filled
    )

    object Activities : HomeTab(
        "Activities",
        R.drawable.ic_tab_activities_outlined,
        R.drawable.ic_tab_activities_filled
    )
}
