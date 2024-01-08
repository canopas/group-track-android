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
        private const val KEY_PHONE_NO = "phone_no"
        private const val PATH = "otp-verification"
        val path = "$PATH/{$KEY_PHONE_NO}"

        fun otpVerification(
            userId: String? = null
        ) = object : AppRoute {

            override val arguments = listOf(
                navArgument(KEY_PHONE_NO) { type = NavType.StringType }
            )

            override val path = "$PATH/$userId"
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