package com.canopas.catchme.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

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