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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodViewModel
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodsScreen
import com.canopas.catchme.ui.flow.auth.phone.EXTRA_RESULT_IS_NEW_USER
import com.canopas.catchme.ui.flow.auth.phone.SignInWithPhoneScreen
import com.canopas.catchme.ui.flow.auth.verification.PhoneVerificationScreen
import com.canopas.catchme.ui.flow.home.HomeScreen
import com.canopas.catchme.ui.flow.intro.IntroScreen
import com.canopas.catchme.ui.flow.onboard.OnboardScreen
import com.canopas.catchme.ui.flow.permission.EnablePermissionsScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.navigation.KEY_RESULT
import com.canopas.catchme.ui.navigation.RESULT_OKAY
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

    AppNavigator(navController = navController, viewModel.navActions)

    NavHost(navController = navController, startDestination = AppDestinations.intro.path) {
        slideComposable(AppDestinations.intro.path) {
            IntroScreen()
        }
        slideComposable(AppDestinations.onboard.path) {
            OnboardScreen()
        }
        slideComposable(AppDestinations.signIn.path) {
            val signInModel = hiltViewModel<SignInMethodViewModel>()

            val result = navController.currentBackStackEntry
                ?.savedStateHandle?.get<Int>(KEY_RESULT)
            result?.let {
                val isNewUSer = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Boolean>(EXTRA_RESULT_IS_NEW_USER) ?: true
                navController.currentBackStackEntry
                    ?.savedStateHandle?.apply {
                        remove<Int>(KEY_RESULT)
                        remove<Boolean>(EXTRA_RESULT_IS_NEW_USER)
                    }

                LaunchedEffect(key1 = result) {
                    if (result == RESULT_OKAY) {
                        signInModel.onSignUp(isNewUSer)
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
        slideComposable(AppDestinations.enablePermissions.path) {
            EnablePermissionsScreen()
        }

        slideComposable(AppDestinations.home.path) {
            HomeScreen()
        }
    }
}
