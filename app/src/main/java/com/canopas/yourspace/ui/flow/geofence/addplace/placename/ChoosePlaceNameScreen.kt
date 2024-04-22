package com.canopas.yourspace.ui.flow.geofence.addplace.placename

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.PrimaryButton
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
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
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
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.choose_place_name_title),
            style = AppTheme.appTypography.header1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.choose_place_name_subtitle),
            style = AppTheme.appTypography.body1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))

        PickNameTextField(
            stringResource(id = R.string.choose_place_name_hint),
            placeName
        ) {
            onPlaceNameChanged(it)
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.home_create_space_suggestions),
            style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Suggestions {
            onPlaceNameChanged(it)
            keyboardController?.hide()
        }
        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.choose_place_btn_add_place),
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
    FlowRow(modifier = Modifier.padding(horizontal = 20.dp), maxItemsInEachRow = 4) {
        suggestion.forEach {
            Text(
                text = it,
                style = AppTheme.appTypography.label2,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .background(AppTheme.colorScheme.containerNormal, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelect(it)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PickNameTextField(title: String, value: String, onValueChanged: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Text(
        text = title.uppercase(),
        style = AppTheme.appTypography.subTitle2.copy()
            .copy(color = AppTheme.colorScheme.textSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        BasicTextField(
            value = value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            textStyle = AppTheme.appTypography.header4.copy(AppTheme.colorScheme.textPrimary),
            onValueChange = { value ->
                onValueChanged(value)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(AppTheme.colorScheme.primary)
        )

        Divider(
            Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = outlineColor
        )
    }
}
