package com.canopas.catchme.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.ui.flow.home.HomeScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.NavAction
import com.canopas.catchme.ui.navigation.onboardNav
import com.canopas.catchme.ui.navigation.slideComposable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<MainViewModel>()

    AppRouter(navController = navController, viewModel.navActions)

    NavHost(navController = navController, startDestination = AppDestinations.home.path) {
        onboardNav(navController)

        slideComposable(AppDestinations.home.path) {
            HomeScreen()
        }
    }
}

@Composable
fun AppRouter(navController: NavHostController, navActions: SharedFlow<NavAction?>) {
    LaunchedEffect(Unit) {
        navActions.collectLatest { action ->
            val navAction = action ?: return@collectLatest

            when (navAction) {
                is NavAction.NavigateBack -> {
                    if (navAction.route != null) {
                        if (navAction.result != null) {
                            navController.getBackStackEntry(navAction.route).savedStateHandle.also { savedStateHandle ->
                                navAction.result.forEach { (key, value) ->
                                    savedStateHandle[key] = value
                                }
                            }
                        }
                        navController.popBackStack(navAction.route, navAction.inclusive)
                    } else {
                        if (navAction.result != null) {
                            navController.previousBackStackEntry?.savedStateHandle.also { savedStateHandle ->
                                navAction.result.forEach { (key, value) ->
                                    savedStateHandle?.set(key, value)
                                }
                            }
                        }
                        navController.popBackStack()
                    }
                }

                is NavAction.NavigateTo -> {
                    navController.navigate(navAction.route) {
                        launchSingleTop = navAction.isSingleTop
                        navAction.popUpToRoute?.let { popUpToRoute ->
                            popUpTo(popUpToRoute) { inclusive = navAction.inclusive }
                        }
                    }
                }
            }
        }
    }
}
