package com.canopas.yourspace.ui.flow.messages.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.component.motionClickEvent
import com.canopas.yourspace.ui.flow.messages.chat.toFormattedTitle
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.utils.formattedMessageTimeString

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
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.loadingSpace || state.loadingThreads) {
            AppProgressIndicator()
        } else if (state.hasMembers) {
            ThreadList(
                state.threadInfo, state.currentSpace?.members ?: emptyList(),
                state.currentUser
            ) { viewModel.showMessages(it) }
        } else {
            NoMemberEmptyContent(state.loadingInviteCode) {
                viewModel.addMember()
            }
        }
    }
}

@Composable
fun ThreadList(
    threadInfos: List<ThreadInfo>,
    members: List<UserInfo>,
    currentUser: ApiUser?,
    onClick: (thread: ThreadInfo) -> Unit
) {
    if (threadInfos.isEmpty()) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            itemsIndexed(
                threadInfos,
                key = { index, item -> item.thread.id }) { index, threadInfo ->
                val threadMembers = members.filter { member ->
                    threadInfo.thread.member_ids.contains(member.user.id) && member.user.id != currentUser?.id
                }.map { it.user }

                ThreadItem(threadInfo, threadMembers, currentUser) { onClick(threadInfo) }
                if (index != threadInfos.size - 1) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = AppTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadItem(
    threadInfo: ThreadInfo,
    members: List<ApiUser>,
    currentUser: ApiUser?,
    onClick: () -> Unit
) {
    val message = threadInfo.messages.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .motionClickEvent { onClick() }
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        ThreadProfile(members)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = members.map { it.first_name ?: "" }.toFormattedTitle(),
                style = AppTheme.appTypography.body1,
                color = AppTheme.colorScheme.textPrimary
            )
            Text(
                text = message?.message ?: "",
                style = AppTheme.appTypography.label2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppTheme.colorScheme.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            val hasUnreadMsg = message?.read_by?.contains(currentUser?.id) == false
            Text(
                text = message?.created_at?.formattedMessageTimeString(
                    LocalContext.current
                ) ?: "",
                style = AppTheme.appTypography.label3,
                color = if (hasUnreadMsg) AppTheme.colorScheme.primary else AppTheme.colorScheme.textSecondary
            )

            if (hasUnreadMsg) {
                val count = threadInfo.messages.count { !it.read_by.contains(currentUser?.id) }
                UnreadIndicator(count)
            }
        }
    }
}

@Composable
fun UnreadIndicator(count: Int) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .height(16.dp)
            .widthIn(min = 16.dp)
            .background(AppTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count <= 10) count.toString() else "10+",
            style = AppTheme.appTypography.label3.copy(fontSize = 10.sp),
            color = AppTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun ThreadProfile(members: List<ApiUser>) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(
                AppTheme.colorScheme.containerNormalOnSurface,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        when (members.size) {
            1 -> {
                members.firstOrNull()?.let {
                    UserProfile(modifier = Modifier.fillMaxSize(), user = it, fontSize = 18.sp)
                }
            }

            2 -> {
                members.forEachIndexed { index, apiUser ->
                    UserProfile(
                        modifier = Modifier
                            .size(34.dp)
                            .border(
                                width = 2.dp,
                                color = AppTheme.colorScheme.containerNormalOnSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(if (index == 0) Alignment.TopStart else Alignment.BottomEnd),
                        user = apiUser,
                        shape = RoundedCornerShape(8.dp),
                        fontSize = 16.sp
                    )
                }
            }

            else -> {
                val filter = members.take(3)
                filter.forEachIndexed { index, apiUser ->
                    UserProfile(
                        modifier = Modifier
                            .size(30.dp)
                            .border(
                                width = 2.dp,
                                color = AppTheme.colorScheme.containerNormalOnSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(if (index == 0) Alignment.TopStart else if (index == 1) Alignment.TopEnd else Alignment.BottomCenter),
                        user = apiUser,
                        shape = RoundedCornerShape(8.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
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
