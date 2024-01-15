package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.catchme.R
import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.component.PrimaryTextButton
import com.canopas.catchme.ui.flow.onboard.OnboardViewModel
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun JoinSpaceOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_join_space_title),
            style = AppTheme.appTypography.header1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Text(
            text = stringResource(
                R.string.onboard_join_space_joining_space_label,
                state.spaceName ?: ""
            ),
            style = AppTheme.appTypography.header4.copy(fontWeight = FontWeight.W500),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_join_space_subtitle),
            style = AppTheme.appTypography.header4.copy(fontWeight = FontWeight.W500),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))

        SpaceMemberComponent(tempList)
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            label = stringResource(R.string.common_btn_join),
            onClick = { viewModel.joinSpace() }, showLoader = state.joiningSpace
        )
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryTextButton(
            label = stringResource(R.string.common_btn_skip),
            onClick = { viewModel.navigateToPermission() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpaceMemberComponent(users: List<ApiUser>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
        ) {
            users.take(4).forEachIndexed { index, user ->
                val setProfile = user.profile_image ?: R.drawable.ic_user_profile_placeholder
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current).data(
                            setProfile
                        ).build()
                    ),
                    modifier = Modifier
                        .padding(start = 45.dp * index)
                        .size(60.dp)
                        .border(3.dp, AppTheme.colorScheme.textPrimary, CircleShape)
                        .clip(CircleShape)
                        .background(AppTheme.colorScheme.containerHigh)
                        .padding(if (user.profile_image == null) 32.dp else 0.dp),
                    contentScale = ContentScale.Crop,
                    contentDescription = "ProfileImage"
                )
            }
            if (users.size > 4) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    modifier = Modifier
                        .padding(start = 180.dp)
                        .size(60.dp)
                        .border(3.dp, AppTheme.colorScheme.textPrimary, CircleShape)
                        .clip(CircleShape)
                        .background(AppTheme.colorScheme.surface.copy(alpha = 1f))
                        .padding(14.dp)
                        .rotate(-90f),
                    tint = AppTheme.colorScheme.textPrimary,
                    contentDescription = "ProfileImage"
                )
            }
        }
    }
}

val tempList = listOf<ApiUser>(
    ApiUser(first_name = "Tom", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tom", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tom hjkk ", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tom r ", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tom sss ", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tommmy", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tomer", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tomyi", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tomlok", profile_image = "https://placebear.com/g/200/200"),
    ApiUser(first_name = "Tomaar", profile_image = "https://placebear.com/g/200/200")
)
