package com.canopas.yourspace.ui.flow.home.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.flow.geofence.addplace.components.PlaceAddedPopup
import com.canopas.yourspace.ui.theme.AppTheme

const val EXTRA_RESULT_PLACE_LATITUDE = "place_latitude"
const val EXTRA_RESULT_PLACE_LONGITUDE = "place_longitude"
const val EXTRA_RESULT_PLACE_NAME = "place_name"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesListScreen() {
    val viewModel = hiltViewModel<PlacesListViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.places_list_title),
                        style = AppTheme.appTypography.header3
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        PlacesListContent(modifier = Modifier.padding(it))
    }

    if (state.placeAdded) {
        val lat = state.addedPlaceLat
        val lng = state.addedPlaceLng
        val name = state.addedPlaceName

        PlaceAddedPopup(lat, lng, name) {
            viewModel.dismissPlaceAddedPopup()
        }
    }
}

@Composable
fun PlacesListContent(modifier: Modifier) {
    val viewModel = hiltViewModel<PlacesListViewModel>()

    Column(modifier = modifier.fillMaxSize()) {
        AddPlaceButton() { viewModel.navigateToAddPlace() }
    }
}

@Composable
fun AddPlaceButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = AppTheme.colorScheme.primary,
                disabledContentColor = AppTheme.colorScheme.textDisabled
            )
        ) {
            Text(
                text = stringResource(id = R.string.places_list_add_new_place_btn),
                style = AppTheme.appTypography.subTitle1
            )
        }
    }
}
