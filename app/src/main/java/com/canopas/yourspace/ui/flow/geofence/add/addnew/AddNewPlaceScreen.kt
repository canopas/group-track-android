package com.canopas.yourspace.ui.flow.geofence.add.addnew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewPlaceScreen() {
    val viewModel = hiltViewModel<AddNewPlaceViewModel>()
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
                        text = stringResource(id = R.string.add_new_place_title),
                        style = AppTheme.appTypography.header3
                    )
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
        AddNewPlace(
            modifier = Modifier.padding(it),
        )
    }
}

@Composable
private fun AddNewPlace(
    modifier: Modifier,
) {
    val viewModel = hiltViewModel<AddNewPlaceViewModel>()
    val state by viewModel.state.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize()) {

        item {
            PlaceNameTextField(
                text = state.searchQuery,
                leadingIcon = R.drawable.ic_tab_places_filled,
                onValueChange = viewModel::onPlaceNameChanged
            )

            Divider(
                Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth(),
                color = AppTheme.colorScheme.outline
            )

            LocateOnMap {
                viewModel.navigateToLocateOnMap()
            }

            PlacesSuggestionHeader()
        }

    }
}

@Composable
private fun PlaceNameTextField(
    text: String,
    enable: Boolean = true,
    leadingIcon: Int,
    maxLines: Int = 1,
    onValueChange: ((value: String) -> Unit) = {}

) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(id = leadingIcon),
                contentDescription = null,
                tint = AppTheme.colorScheme.onDisabled,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.fillMaxSize()){
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.add_new_place_search_place_hint),
                        style = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textDisabled)
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { onValueChange(it) },
                    maxLines = maxLines,
                    enabled = enable,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxWidth(),
                    textStyle = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    cursorBrush = SolidColor(AppTheme.colorScheme.primary)
                )
            }

        }

        Divider(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = outlineColor
        )
    }
}


@Composable
fun PlacesSuggestionHeader() {
    Text(
        text = stringResource(id = R.string.add_new_place_header_suggestions),
        style = AppTheme.appTypography.header3,
        color = AppTheme.colorScheme.textSecondary,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(color = AppTheme.colorScheme.containerLow)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp)
    )
}

@Composable
private fun LocateOnMap(onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(0.5.dp, AppTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_geofence),
                contentDescription = null,
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.locate_on_map_title),
            style = AppTheme.appTypography.subTitle1,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        )
    }
}
