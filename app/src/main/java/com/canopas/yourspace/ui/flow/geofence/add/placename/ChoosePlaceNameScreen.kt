package com.canopas.yourspace.ui.flow.geofence.add.placename

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.SearchTextField
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoosePlaceNameScreen() {
    val viewModel = hiltViewModel<ChoosePlaceNameViewModel>()
    val state by viewModel.state.collectAsState()

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.choose_place_name_title),
                        style = AppTheme.appTypography.subTitle1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) {
        CreatePlace(
            modifier = Modifier.padding(it),
            placeName = state.placeName,
            showLoader = state.addingPlace,
            onPlaceNameChanged = { viewModel.onPlaceNameChange(it) },
            onNext = {
                viewModel.addPlace()
            }
        )
    }
}

@Composable
fun CreatePlace(
    modifier: Modifier = Modifier,
    placeName: String = "",
    showLoader: Boolean = false,
    onPlaceNameChanged: (String) -> Unit = {},
    onNext: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SearchTextField(
            text = placeName,
            hint = stringResource(id = R.string.choose_place_name_hint),
            onValueChange = onPlaceNameChanged
        )

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.home_create_space_suggestions),
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Suggestions {
            onPlaceNameChanged(it)
            keyboardController?.hide()
        }
        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.common_btn_add_place),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                onNext()
                keyboardController?.hide()
            },
            enabled = placeName.trim().isNotEmpty(),
            showLoader = showLoader
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(onSelect: (String) -> Unit) {
    val suggestion = stringArrayResource(id = R.array.choose_place_name_suggestion)
    FlowRow(modifier = Modifier.padding(horizontal = 16.dp), maxItemsInEachRow = 4) {
        suggestion.forEach {
            Text(
                text = it,
                style = AppTheme.appTypography.body2,
                color = AppTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .background(AppTheme.colorScheme.containerLow, CircleShape)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable { onSelect(it) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
