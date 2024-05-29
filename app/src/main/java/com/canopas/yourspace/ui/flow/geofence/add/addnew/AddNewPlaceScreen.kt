package com.canopas.yourspace.ui.flow.geofence.add.addnew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.SearchTextField
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.libraries.places.api.model.Place

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
        AddNewPlace(
            modifier = Modifier.padding(it)
        )
    }
}

@Composable
private fun AddNewPlace(
    modifier: Modifier
) {
    val viewModel = hiltViewModel<AddNewPlaceViewModel>()
    val state by viewModel.state.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SearchTextField(
                text = state.searchQuery,
                hint = stringResource(id = R.string.add_new_place_search_place_hint),
                onValueChange = viewModel::onPlaceNameChanged
            )

            Spacer(modifier = Modifier.height(40.dp))

            LocateOnMap {
                viewModel.navigateToLocateOnMap()
            }
        }
        if (state.places.isNotEmpty() || state.loading) {
            item {
                PlacesSuggestionHeader()
            }
        }

        items(state.places) {
            PlaceSuggestionItem(
                place = it,
                onClick = { viewModel.onPlaceSelected(it) }
            )
        }

        if (state.loading) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AppProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PlaceSuggestionItem(
    place: Place,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(color = AppTheme.colorScheme.containerLow, shape = CircleShape)
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tab_places_outlined),
            contentDescription = null,
            tint = AppTheme.colorScheme.textPrimary,
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Text(
                text = place.name ?: "",
                maxLines = 1,
                style = AppTheme.appTypography.body2,
                color = AppTheme.colorScheme.textSecondary,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.address ?: "",
                style = AppTheme.appTypography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlacesSuggestionHeader() {
    Text(
        text = stringResource(id = R.string.add_new_place_header_suggestions),
        style = AppTheme.appTypography.caption,
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 40.dp, bottom = 16.dp)
    )
}

@Composable
private fun LocateOnMap(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(60.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(
                color = AppTheme.colorScheme.containerLow,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppTheme.colorScheme.containerLow,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_places_outlined),
                contentDescription = null,
                tint = AppTheme.colorScheme.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.locate_on_map_title),
            style = AppTheme.appTypography.subTitle2,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        )
    }
}
