package com.canopas.yourspace.ui.flow.settings.space.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun ChangeAdminScreen(space: SpaceInfo) {
    val viewModel = hiltViewModel<ChangeAdminViewModel>()
    val state by viewModel.state.collectAsState()
    var selectedUserId by remember { mutableStateOf(space.space.admin_id) }

    LaunchedEffect(Unit) {
        if (space.space.id.isNotEmpty()) {
            viewModel.fetchSpaceDetail(space.space.id)
            state.spaceID = space.space.id
            selectedUserId = space.space.admin_id
        }
    }

    Scaffold(
        topBar = { ChangeAdminAppBar(selectedUserId) }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                SpaceMemberListContent(
                    userInfo = space.members,
                    onUserSelect = { userId -> selectedUserId = userId },
                    selectedUserId = selectedUserId
                )
                if (state.isLoading) {
                    AppProgressIndicator()
                }
            } else {
                NoInternetScreen(viewModel::checkInternetConnection)
            }
        }
    }
}

@Composable
fun SpaceMemberListContent(
    userInfo: List<UserInfo>,
    onUserSelect: (String) -> Unit,
    selectedUserId: String?
) {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            userInfo.forEach { user ->
                UserItem(
                    userInfo = user,
                    isSelected = selectedUserId == user.user.id,
                    onUserSelect = onUserSelect
                )
            }
        }
    }
}

@Composable
private fun UserItem(
    userInfo: UserInfo,
    isSelected: Boolean,
    onUserSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        UserProfile(modifier = Modifier.size(40.dp), user = userInfo.user)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = userInfo.user.fullName,
            style = AppTheme.appTypography.subTitle2,
            color = AppTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )

        RadioButton(
            selected = isSelected,
            onClick = { onUserSelect(userInfo.user.id) },
            enabled = true,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppTheme.colorScheme.primary,
                unselectedColor = AppTheme.colorScheme.textDisabled
            ),
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeAdminAppBar(selectedUserId: String?) {
    val viewModel = hiltViewModel<ChangeAdminViewModel>()

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {},
        navigationIcon = {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                        contentDescription = "",
                        tint = AppTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(id = R.string.common_btn_back),
                    color = AppTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        actions = {
            IconButton(
                onClick = { selectedUserId?.let { viewModel.changeAdmin(it) } }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = ""
                )
            }
        }
    )
}
