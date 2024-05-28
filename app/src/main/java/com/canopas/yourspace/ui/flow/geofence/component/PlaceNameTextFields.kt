package com.canopas.yourspace.ui.flow.geofence.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaceNameContent(
    placeName: String,
    cameraPositionState: CameraPositionState,
    userLocation: LatLng,
    enable: Boolean = false,
    onPlaceNameChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }

    LaunchedEffect(cameraPositionState.isMoving) {
        withContext(Dispatchers.IO) {
            if (!cameraPositionState.isMoving) {
                address = ""
                address = cameraPositionState.position.target.getAddress(context) ?: ""
            }
        }
    }

    LaunchedEffect(key1 = userLocation) {
        withContext(Dispatchers.IO) {
            if (address.isEmpty()) {
                address = userLocation.getAddress(context) ?: ""
            }
        }
    }

    PlaceNameTextField(
        placeName,
        leadingIcon = R.drawable.ic_bookmark,
        onValueChange = onPlaceNameChanged,
        enable = enable
    )

    Spacer(modifier = Modifier.height(16.dp))

    PlaceNameTextField(
        address.ifEmpty { stringResource(id = R.string.locate_on_map_hint_getting_address) },
        leadingIcon = R.drawable.ic_tab_places_outlined,
        maxLines = 2,
        enable = false
    )
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
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(id = leadingIcon),
                contentDescription = null,
                tint = AppTheme.colorScheme.textPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = text,
                onValueChange = { onValueChange(it) },
                maxLines = maxLines,
                singleLine = true,
                enabled = enable,
                interactionSource = interactionSource,
                textStyle = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary),
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

        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = outlineColor
        )
    }
}
