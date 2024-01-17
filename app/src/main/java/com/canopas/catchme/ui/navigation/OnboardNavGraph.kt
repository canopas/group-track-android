package com.canopas.catchme.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodViewModel
import com.canopas.catchme.ui.flow.auth.methods.SignInMethodsScreen
import com.canopas.catchme.ui.flow.auth.permission.EnablePermissionsScreen
import com.canopas.catchme.ui.flow.auth.phone.EXTRA_RESULT_IS_NEW_USER
import com.canopas.catchme.ui.flow.auth.phone.SignInWithPhoneScreen
import com.canopas.catchme.ui.flow.auth.verification.PhoneVerificationScreen
import com.canopas.catchme.ui.flow.intro.IntroScreen
import com.canopas.catchme.ui.flow.onboard.OnboardScreen

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.onboardNav(
    navController: NavHostController
) {
    navigation(
        startDestination = AppDestinations.intro.path,
        route = "app-onboard"
    ) {
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
    }
}
