package com.canopas.yourspace.ui.flow.onboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.ui.flow.onboard.components.CreateSpaceOnboard
import com.canopas.yourspace.ui.flow.onboard.components.JoinOrCreateSpaceOnboard
import com.canopas.yourspace.ui.flow.onboard.components.PickNameOnboard
import com.canopas.yourspace.ui.flow.onboard.components.ShareSpaceCodeOnboard
import com.canopas.yourspace.ui.flow.onboard.components.SpaceInfoOnboard

@Composable
fun OnboardScreen() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()

    AnimatedVisibility(
        visible = state.currentStep == OnboardItems.PickName,
        enter = EnterTransition.None,
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        PickNameOnboard()
    }

    AnimatedVisibility(
        visible = state.currentStep == OnboardItems.SpaceIntro,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        SpaceInfoOnboard()
    }

    AnimatedVisibility(
        visible = state.currentStep == OnboardItems.JoinOrCreateSpace,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        JoinOrCreateSpaceOnboard()
    }

    AnimatedVisibility(
        visible = state.currentStep == OnboardItems.CreateSpace,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = {
            if (state.prevStep == OnboardItems.CreateSpace) it else -it
        })
    ) {
        CreateSpaceOnboard()
    }

    AnimatedVisibility(
        visible = state.currentStep == OnboardItems.ShareSpaceCodeOnboard,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        ShareSpaceCodeOnboard()
    }
}
