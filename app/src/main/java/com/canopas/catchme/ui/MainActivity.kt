package com.canopas.catchme.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodsScreen
import com.canopas.catchme.ui.flow.auth.phone.SignInWithPhoneScreen
import com.canopas.catchme.ui.flow.auth.verification.PhoneVerificationScreen
import com.canopas.catchme.ui.flow.intro.IntroScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.KEY_RESULT
import com.canopas.catchme.ui.navigation.NavAction
import com.canopas.catchme.ui.navigation.RESULT_OKAY
import com.canopas.catchme.ui.navigation.slideComposable
import com.canopas.catchme.ui.theme.CatchMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatchMeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<MainViewModel>()

    AppRouter(navController = navController, viewModel.navActions)

    NavHost(navController = navController, startDestination = AppDestinations.home.path) {
        slideComposable(AppDestinations.intro.path) {
            IntroScreen()
        }
        slideComposable(AppDestinations.signIn.path) {

            val result = navController.currentBackStackEntry
                ?.savedStateHandle?.get<Int>(KEY_RESULT)
            result?.let {
                navController.currentBackStackEntry
                    ?.savedStateHandle?.remove<Int>(KEY_RESULT)
                LaunchedEffect(key1 = result) {
                    if (result == RESULT_OKAY) {
                        navController.navigate(AppDestinations.home.path) {
                            popUpTo(AppDestinations.intro.path) { inclusive = true }
                        }
                    }
                }
            }
            SignInMethodsScreen()
        }

        slideComposable(AppDestinations.phoneSignIn.path) {
            SignInWithPhoneScreen()
        }

        slideComposable(AppDestinations.OtpVerificationNavigation.path) {
            PhoneVerificationScreen()
        }

        slideComposable(AppDestinations.home.path) {

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
