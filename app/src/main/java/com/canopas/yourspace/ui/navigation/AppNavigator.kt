package com.canopas.yourspace.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val KEY_RESULT = "result_code"
const val RESULT_OKAY = 1
const val RESULT_CANCEL = 0

@Singleton
class AppNavigator @Inject constructor() {

    private val _navigationChannel =
        MutableSharedFlow<NavAction>(extraBufferCapacity = 1)
    val navigationChannel = _navigationChannel.asSharedFlow()

    fun navigateBack(
        route: String? = null,
        inclusive: Boolean = false,
        result: Map<String, Any>? = null
    ) {
        _navigationChannel.tryEmit(
            NavAction.NavigateBack(
                route = route,
                inclusive = inclusive,
                result = result
            )
        )
    }

    fun navigateTo(
        route: String,
        popUpToRoute: String? = null,
        inclusive: Boolean = false,
        clearStack: Boolean = false
    ) {
        _navigationChannel.tryEmit(
            NavAction.NavigateTo(
                route = route,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive,
                clearStack = clearStack
            )
        )
    }
}

sealed class NavAction {

    data class NavigateBack(
        val route: String? = null,
        val inclusive: Boolean = false,
        val result: Map<String, Any?>? = null
    ) : NavAction()

    data class NavigateTo(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val clearStack: Boolean = false
    ) : NavAction()
}

@Composable
fun AppNavigator(navController: NavHostController, navActions: SharedFlow<NavAction?>) {
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
                        if (navAction.clearStack) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        } else {
                            navAction.popUpToRoute?.let { popUpToRoute ->
                                popUpTo(popUpToRoute) { inclusive = navAction.inclusive }
                            }
                        }
                    }
                }
            }
        }
    }
}
