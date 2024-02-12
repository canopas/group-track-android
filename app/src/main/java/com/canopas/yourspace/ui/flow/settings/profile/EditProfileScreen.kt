package com.canopas.yourspace.ui.flow.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.flow.settings.profile.component.UserProfileView
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun EditProfileScreen() {

    Scaffold(
        topBar = {
            EditProfileToolbar()
        }
    ) {
        EditProfileScreenContent(Modifier.padding(it))
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileToolbar() {
    val viewModel = hiltViewModel<EditProfileViewModel>()
    val state by viewModel.state.collectAsState()
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {
            Text(
                text = stringResource(id = R.string.edit_profile_title),
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
        },
        actions = {
            Text(
                text = stringResource(id = R.string.edit_profile_toolbar_save_text),
                color = if (state.allowSave) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
                style = AppTheme.appTypography.subTitle1,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        enabled = state.allowSave,
                        onClick = {
                            viewModel.saveUser()
                        }
                    )
            )
        }
    )
}

@Composable
private fun EditProfileScreenContent(modifier: Modifier) {
    val viewModel = hiltViewModel<EditProfileViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            UserProfileView(
                Modifier.align(Alignment.CenterHorizontally),
                state.profileUrl,
                onProfileChanged = {
                    viewModel.onProfileImageChanged(it)
                },
                onProfileImageClicked = {
                    viewModel.showProfileChooser()
                },
                dismissProfileChooser = {
                    viewModel.showProfileChooser(false)
                },
                state.showProfileChooser
            )
        }


        if (state.loading) {
            AppProgressIndicator()
        }

        if (state.error != null) {
            AppBanner(msg = state.error!!) {
                viewModel.resetErrorState()
            }
        }

    }
}

}

@Composable
private fun UserTextField(
    label: String,
    text: String,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    ),
    onValueChange: (value: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
            style = AppTheme.appTypography.body2
        )

        CustomTextField(
            text = text,
            onTextChange = { onValueChange(it) },
            enabled = enabled,
            maxLines = 1,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            cursorBrush = SolidColor(AppTheme.colorScheme.primary)
        )
        Divider(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
           color = AppTheme.colorScheme.outline
        )
    }
}
