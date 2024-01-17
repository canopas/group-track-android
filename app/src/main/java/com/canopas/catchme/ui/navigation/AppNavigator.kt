package com.canopas.catchme.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    val onboard = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "onboard"
    }

    val enablePermissions = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "enable-permissions"
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
        const val path = "$PATH/{$KEY_PHONE_NO}/{$KEY_VERIFICATION_ID}"

        fun otpVerification(
            verificationId: String,
            phoneNo: String
        ) = object : AppRoute {

            override val arguments = listOf(
                navArgument(KEY_PHONE_NO) { type = NavType.StringType },
                navArgument(KEY_VERIFICATION_ID) { type = NavType.StringType }
            )

            override val path = "$PATH/$phoneNo/$verificationId"
        }
    }

    val map = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "map"
    }

    val places = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "places"
    }

    val activity = object : AppRoute {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val path: String = "activity"
    }
}

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
        isSingleTop: Boolean = false
    ) {
        _navigationChannel.tryEmit(
            NavAction.NavigateTo(
                route = route,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive,
                isSingleTop = isSingleTop
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
        val isSingleTop: Boolean = false
    ) : NavAction()
}
