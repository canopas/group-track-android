package com.canopas.catchme.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodsScreen
import com.canopas.catchme.ui.flow.auth.phone.SignInWithPhoneScreen
import com.canopas.catchme.ui.flow.auth.verification.PhoneVerificationScreen
import com.canopas.catchme.ui.flow.intro.IntroScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.NavAction
import com.canopas.catchme.ui.navigation.slideComposable
import com.canopas.catchme.ui.theme.CatchMeTheme
import dagger.hilt.android.AndroidEntryPoint

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

    val appState by viewModel.appState.collectAsState()

    viewModel.navActions.collectAsState().value.also { navAction ->
        if (navAction == null) return@also
        when (navAction) {
            is NavAction.NavigateBack -> {
                if (navAction.route != null) {
                    navController.popBackStack(navAction.route, navAction.inclusive)
                } else {
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

    NavHost(navController = navController, startDestination = AppDestinations.signIn.path) {
        slideComposable(AppDestinations.intro.path) {
            IntroScreen()
        }
        slideComposable(AppDestinations.signIn.path) {
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
