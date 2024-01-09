package com.canopas.catchme.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AppRoute {
    val arguments: List<NamedNavArgument>

    val path: String
}

object AppDestinations {
    val home = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "home"
    }

    val intro = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "intro"
    }

    val signIn = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "sign-in"
    }

    val phoneSignIn = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "phone-sign-in"
    }

    object OtpVerificationNavigation {
        const val KEY_PHONE_NO = "phone_no"
        const val KEY_VERIFICATION_ID = "verification_id"

        private const val PATH = "otp-verification"
        val path = "$PATH/{$KEY_PHONE_NO}/{$KEY_VERIFICATION_ID}"

        fun otpVerification(
            verificationId: String,
            phoneNo: String,
        ) = object : AppRoute {

            override val arguments = listOf(
                navArgument(KEY_PHONE_NO) { type = NavType.StringType },
                navArgument(KEY_VERIFICATION_ID) { type = NavType.StringType }
            )

            override val path = "$PATH/$phoneNo/$verificationId"
        }
    }
}

@Singleton
class AppNavigator @Inject constructor() {
    val navigationChannel = MutableStateFlow<NavAction?>(null)

    suspend fun navigateBack(route: String? = null, inclusive: Boolean = false) {
        navigationChannel.emit(
            NavAction.NavigateBack(
                route = route,
                inclusive = inclusive
            )
        )
    }

    suspend fun navigateTo(
        route: String,
        popUpToRoute: String? = null,
        inclusive: Boolean = false,
        isSingleTop: Boolean = false
    ) {
        navigationChannel.emit(
            NavAction.NavigateTo(
                route = route,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }
}

sealed class NavAction {

    data class NavigateBack(
        val route: String? = null,
        val inclusive: Boolean = false,
    ) : NavAction()

    data class NavigateTo(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = false,
    ) : NavAction()
}