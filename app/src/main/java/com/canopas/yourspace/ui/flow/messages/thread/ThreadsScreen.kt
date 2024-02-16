package com.canopas.yourspace.ui.flow.messages.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen() {
    val viewModel = hiltViewModel<ThreadsViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    if (state.currentSpace != null) {
                        Column {
                            Text(
                                text = state.currentSpace?.space?.name ?: "",
                                style = AppTheme.appTypography.header3
                            )
                            Text(
                                text = stringResource(id = R.string.threads_screen_subtitle_messages),
                                style = AppTheme.appTypography.body3
                            )
                        }
                    }
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
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface,
        floatingActionButton = {
            if (state.hasMembers) {
                FloatingActionButton(
                    onClick = { viewModel.createNewThread() },
                    containerColor = AppTheme.colorScheme.primary,
                    contentColor = AppTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Floating action button.")
                }
            }
        }
    ) {
        ThreadsContent(modifier = Modifier.padding(it))
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}

@Composable
private fun ThreadsContent(modifier: Modifier) {
    val viewModel = hiltViewModel<ThreadsViewModel>()
    val state by viewModel.state.collectAsState()

    if (state.loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppProgressIndicator()
        }
    } else if (state.hasMembers) {
        ThreadList(state.threadInfo)
    } else {
        NoMemberEmptyContent(state.loadingInviteCode) {
            viewModel.addMember()
        }
    }
}

@Composable
fun ThreadList(threadInfo: List<ThreadInfo>) {
    if (threadInfo.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.threads_screen_no_threads),
                textAlign = TextAlign.Center,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )
        }
    } else {
        LazyColumn() {
            items(threadInfo.size) {
                ThreadItem(threadInfo[it])
            }
        }
    }
}

@Composable
private fun ThreadItem(threadInfo: ThreadInfo) {
}

@Composable
private fun NoMemberEmptyContent(
    loadingInviteCode: Boolean,
    addMember: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_thread_no_member),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = AppTheme.colorScheme.textPrimary
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_title),
            style = AppTheme.appTypography.header4
        )
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_subtitle),
            style = AppTheme.appTypography.label1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.padding(10.dp))

        PrimaryButton(
            label = stringResource(id = R.string.thread_screen_add_new_member),
            onClick = addMember,
            showLoader = loadingInviteCode
        )
    }
}
