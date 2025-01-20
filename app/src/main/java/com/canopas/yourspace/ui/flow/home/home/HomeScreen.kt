package com.canopas.yourspace.ui.flow.home.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.yourspace.R
import com.canopas.yourspace.data.utils.isBackgroundLocationPermissionGranted
import com.canopas.yourspace.data.utils.isBatteryOptimizationEnabled
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.ActionIconButton
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.PermissionDialog
import com.canopas.yourspace.ui.flow.geofence.places.PlacesListScreen
import com.canopas.yourspace.ui.flow.home.activity.ActivityScreen
import com.canopas.yourspace.ui.flow.home.home.component.SpaceSelectionMenu
import com.canopas.yourspace.ui.flow.home.home.component.SpaceSelectionPopup
import com.canopas.yourspace.ui.flow.home.map.MapScreen
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.tabComposable
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun HomeScreen(verifyingSpace: Boolean) {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (context.isBackgroundLocationPermissionGranted) {
            viewModel.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (context.isBatteryOptimizationEnabled && context.isBackgroundLocationPermissionGranted) {
            viewModel.showBatteryOptimizationDialog()
        }
    }

    Scaffold(
        containerColor = AppTheme.colorScheme.surface,
        content = {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                Box(
                    modifier = Modifier
                        .padding(it)
                ) {
                    HomeScreenContent(navController, viewModel::dismissSpaceSelection)

                    HomeTopBar(verifyingSpace)
                }
            } else {
                NoInternetScreen(viewModel::observeConnectivity)
            }
        }
        /* bottomBar = {
              AnimatedVisibility(
                  visible = !hideBottomBar,
                  enter = slideInVertically(tween(100)) { it },
                  exit = slideOutVertically(tween(100)) { it }
              ) {
                  HomeBottomBar(navController)
              }
          }*/
    )
}

@Composable
fun HomeTopBar(verifyingSpace: Boolean) {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    SpaceSelectionPopup(
        show = state.showSpaceSelectionPopup,
        spaces = state.spaces,
        selectSpaceId = state.selectedSpaceId,
        onSpaceSelected = {
            viewModel.selectSpace(it)
        },
        onJoinSpace = { viewModel.joinSpace() },
        onCreateSpace = {
            viewModel.navigateToCreateSpace()
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = AppTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!state.showSpaceSelectionPopup) {
            MapControl(icon = R.drawable.ic_settings) {
                viewModel.navigateToSettings()
            }
        }

        SpaceSelectionMenu(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            verifyingSpace = verifyingSpace,
            isExpand = state.showSpaceSelectionPopup
        )

        if (!state.selectedSpaceId.isNullOrEmpty() && !state.showSpaceSelectionPopup) {
            MapControl(icon = R.drawable.ic_messages) {
                viewModel.navigateToThreads()
            }
        }

        Box {
            if (state.showSpaceSelectionPopup) {
                MapControl(icon = R.drawable.ic_add_member) {
                    viewModel.addMember()
                }
            } else if (!state.selectedSpaceId.isNullOrEmpty()) {
                MapControl(
                    icon = if (state.locationEnabled) R.drawable.ic_location_on else R.drawable.ic_location_off,
                    showLoader = state.enablingLocation
                ) {
                    viewModel.toggleLocation()
                }
            }
        }
    }

    if (state.showBatteryOptimizationPopup) {
        val context = LocalContext.current
        PermissionDialog(
            title = stringResource(R.string.battery_optimization_dialog_title),
            subTitle1 = stringResource(R.string.battery_optimization_dialog_message),
            dismissBtn = stringResource(R.string.common_btn_cancel),
            confirmBtn = stringResource(R.string.battery_optimization_dialog_btn_change_now),
            onDismiss = { viewModel.dismissBatteryOptimizationDialog() },
            goToSettings = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                viewModel.dismissBatteryOptimizationDialog()
            }
        )
    }
}

@Composable
private fun MapControl(
    @DrawableRes icon: Int,
    visible: Boolean = true,
    showLoader: Boolean = false,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        tween(durationMillis = 100),
        label = ""
    )

    ActionIconButton(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer(alpha = alpha),
        icon = icon,
        showLoader = showLoader,
        containerColor = AppTheme.colorScheme.containerNormalOnSurface,
        contentColor = AppTheme.colorScheme.textPrimary,
        onClick = onClick
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreenContent(navController: NavHostController, dismissSpaceSelection: () -> Unit) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.map.path
    ) {
        tabComposable(AppDestinations.map.path) {
            MapScreen(dismissSpaceSelection)
        }

        tabComposable(AppDestinations.places.path) {
            PlacesListScreen()
        }

        tabComposable(AppDestinations.activity.path) {
            ActivityScreen()
        }
    }
}

@Composable
fun HomeBottomBar(navController: NavHostController) {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BottomAppBar(
        contentColor = AppTheme.colorScheme.primary,
        containerColor = AppTheme.colorScheme.surface,
        modifier = Modifier.shadow(10.dp)
    ) {
        NavItem(
            HomeTab.Main,
            state.currentTab == 0
        ) {
            navigateTo(AppDestinations.map.path)
            viewModel.onTabChange(0)
        }

        NavItem(
            HomeTab.Places,
            state.currentTab == 1
        ) {
            navigateTo(AppDestinations.places.path)
            viewModel.onTabChange(1)
        }

        NavItem(
            HomeTab.Activities,
            state.currentTab == 2
        ) {
            navigateTo(AppDestinations.activity.path)
            viewModel.onTabChange(2)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    screen: HomeTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (screen.route) {
        HomeTab.Main.route -> stringResource(R.string.home_tab_label_home)
        HomeTab.Places.route -> stringResource(R.string.home_tab_label_places)
        HomeTab.Activities.route -> stringResource(R.string.home_tab_label_activities)
        else -> stringResource(R.string.home_tab_label_home)
    }

    NavigationBarItem(
        icon = {
            Icon(
                painter = painterResource(id = if (isSelected) screen.resourceIdFilled else screen.resourceIdLine),
                null,
                modifier = Modifier.size(24.dp)
            )
        },
        selected = isSelected,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = AppTheme.colorScheme.containerNormalOnSurface,
            selectedIconColor = AppTheme.colorScheme.primary,
            selectedTextColor = AppTheme.colorScheme.primary,
            unselectedIconColor = AppTheme.colorScheme.textDisabled,
            unselectedTextColor = AppTheme.colorScheme.textDisabled
        ),
        alwaysShowLabel = true,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTheme.appTypography.label3,
                fontSize = 10.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

sealed class HomeTab(
    val route: String,
    @DrawableRes val resourceIdLine: Int,
    @DrawableRes val resourceIdFilled: Int
) {
    data object Main : HomeTab(
        "Main",
        R.drawable.ic_tab_home_outlined,
        R.drawable.ic_tab_home_filled
    )

    data object Places : HomeTab(
        "Places",
        R.drawable.ic_tab_places_outlined,
        R.drawable.ic_tab_places_filled
    )

    data object Activities : HomeTab(
        "Activities",
        R.drawable.ic_tab_activities_outlined,
        R.drawable.ic_tab_activities_filled
    )
}

val showSpaceTopBarOn: List<String>
    get() = listOf(
        AppDestinations.map.path,
        AppDestinations.activity.path,
        AppDestinations.places.path
    )

val hideBottomBarOn: List<String>
    get() = listOf(
        AppDestinations.createSpace.path,
        AppDestinations.SpaceInvitation.path,
        AppDestinations.joinSpace.path
    )
