package com.canopas.yourspace.ui.flow.messages.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.formattedMessageDateHeader
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.component.reachedBottom
import com.canopas.yourspace.ui.flow.messages.chat.toFormattedTitle
import com.canopas.yourspace.ui.theme.AppTheme
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.MessageList(
    lazyState: LazyListState,
    loading: Boolean,
    append: Boolean,
    messagesByDate: Map<Long, List<ApiThreadMessage>>,
    newMessagesToAppend: List<ApiThreadMessage>,
    members: List<UserInfo>,
    currentUserId: String,
    loadMore: () -> Unit
) {
    val reachedBottom by remember {
        derivedStateOf { lazyState.reachedBottom() }
    }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom && messagesByDate.isNotEmpty()) {
            loadMore()
        }
    }

    LazyColumn(
        state = lazyState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        reverseLayout = true
    ) {
        itemsIndexed(newMessagesToAppend, key = { index, item -> item.id }) { index, message ->
            val by = members.firstOrNull { it.user.id == message.sender_id }

            val myLatestMsg =
                newMessagesToAppend.firstOrNull { it.sender_id == currentUserId }?.id == message.id
            MessageContent(
                previousMessage = if (index < newMessagesToAppend.size - 1) newMessagesToAppend[index + 1] else null,
                nextMessage = if (index > 0 && index < newMessagesToAppend.size - 1) newMessagesToAppend[index - 1] else null,
                message,
                by = by,
                seenBy = emptyList(),
                isGroupChat = members.size > 2,
                isSender = true,
                isLatestMsg = myLatestMsg
            )
        }

        messagesByDate.forEach { section ->
            val messages = section.value

            itemsIndexed(messages, key = { index, item -> item.id }) { index, message ->
                val by = members.firstOrNull { it.user.id == message.sender_id }

                val seenBy =
                    members.filter { message.seen_by.contains(it.user.id) && it.user.id != currentUserId }

                val myLatestMsg =
                    messages.firstOrNull { it.sender_id == currentUserId }?.id == message.id
                MessageContent(
                    previousMessage = if (index < messages.size - 1) messages[index + 1] else null,
                    nextMessage = if (index > 0 && index < messages.size - 1) messages[index - 1] else null,
                    message,
                    by = by,
                    seenBy = seenBy,
                    isGroupChat = members.size > 2,
                    isSender = currentUserId == message.sender_id,
                    isLatestMsg = myLatestMsg
                )
            }
            if (members.isNotEmpty()) {
                item {
                    Text(
                        text = section.key.formattedMessageDateHeader(LocalContext.current),
                        style = AppTheme.appTypography.body1.copy(color = AppTheme.colorScheme.textSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (append && !loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) { AppProgressIndicator() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.MessageContent(
    previousMessage: ApiThreadMessage?,
    nextMessage: ApiThreadMessage?,
    message: ApiThreadMessage,
    by: UserInfo?,
    seenBy: List<UserInfo>,
    isGroupChat: Boolean,
    isSender: Boolean,
    isLatestMsg: Boolean
) {
    val showUserDetails = shouldShowUserDetails(previousMessage, message)
    val isPreviousUser = previousMessage?.sender_id == message.sender_id
    val lastMessage = nextMessage?.sender_id != message.sender_id
    val userName =
        if (!isSender && showUserDetails && isGroupChat) by?.user?.first_name ?: "" else ""

    Row(
        Modifier
            .padding(top = if (showUserDetails) 24.dp else 4.dp)
            .fillMaxWidth()
            .animateItemPlacement(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        if (!isSender && showUserDetails) {
            UserProfile(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(24.dp),
                placeholderSize = 16.dp,
                user = by?.user
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        val seenLabel = if (isSender && seenBy.isNotEmpty() && isLatestMsg) {
            if (isGroupChat) {
                stringResource(
                    R.string.messages_label_seen_by,
                    seenBy.map { it.user.first_name ?: "" }.toFormattedTitle()
                )
            } else {
                stringResource(R.string.messages_label_seen)
            }
        } else {
            ""
        }

        MessageBubble(
            isSent = message.isSent,
            message = message.message,
            timeLabel = if (!showUserDetails) "" else message.formattedTime,
            seenLabel = seenLabel,
            isSender = isSender,
            isSameUser = isPreviousUser,
            isLastMsg = lastMessage,
            userName = userName
        )
    }
}

fun shouldShowUserDetails(previousMessage: ApiThreadMessage?, message: ApiThreadMessage): Boolean {
    if (previousMessage == null) return true
    val diff = message.createdAtMs - previousMessage.createdAtMs

    // Check if the previous message is from the same sender and within a minute
    return previousMessage.sender_id != message.sender_id || diff !in 0..TimeUnit.MINUTES.toMillis(1)
}

@Composable
fun MessageBubble(
    isSent: Boolean,
    message: String,
    timeLabel: String,
    seenLabel: String,
    isSender: Boolean,
    isSameUser: Boolean,
    isLastMsg: Boolean,
    userName: String = ""
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val align = if (isSender) Alignment.End else Alignment.Start
    val containerColor = if (isSender) {
        AppTheme.colorScheme.primary.copy(alpha = if (isSent) 0.7f else 0.5f)
    } else {
        AppTheme.colorScheme.containerLow
    }

    val shape = if (isSender) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = if (!isSameUser) 16.dp else 2.dp,
            bottomStart = 16.dp,
            bottomEnd = if (isLastMsg) 16.dp else 2.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (!isSameUser) 16.dp else 2.dp,
            topEnd = 16.dp,
            bottomStart = if (isLastMsg) 16.dp else 2.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(max = screenWidth * 0.8f, min = 100.dp)
    ) {
        if (timeLabel.isNotEmpty() && isSent) {
            Text(
                text = timeLabel,
                style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled),
                modifier = Modifier
                    .align(align)
                    .padding(bottom = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .background(color = containerColor, shape = shape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(align)
        ) {
            if (userName.isNotEmpty()) {
                Text(
                    text = userName,
                    style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.successColor),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = message,
                style = AppTheme.appTypography.subTitle3.copy(color = AppTheme.colorScheme.textPrimary)
            )
        }

        if (seenLabel.isNotEmpty()) {
            Text(
                text = seenLabel,
                style = AppTheme.appTypography.label3.copy(color = AppTheme.colorScheme.textDisabled),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .widthIn(max = screenWidth * 0.3f)
                    .align(align),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
