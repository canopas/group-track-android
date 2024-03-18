package com.canopas.yourspace.ui.flow.messages.chat.components

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.component.reachedBottom
import com.canopas.yourspace.ui.flow.messages.chat.toFormattedTitle
import com.canopas.yourspace.ui.theme.AppTheme
import timber.log.Timber

@Composable
fun ColumnScope.MessageList(
    loading: Boolean,
    append: Boolean,
    messages: List<ApiThreadMessage>,
    members: List<UserInfo>,
    currentUserId: String,
    loadMore: () -> Unit
) {
    val lazyState = rememberLazyListState()
    val reachedBottom by remember {
        derivedStateOf { lazyState.reachedBottom() }
    }
    LaunchedEffect(reachedBottom) {
        if (reachedBottom) loadMore()
    }

    LazyColumn(
        state = lazyState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        reverseLayout = true
    ) {
        itemsIndexed(messages) { index, message ->
            val by =
                members.firstOrNull { it.user.id == message.sender_id }

            val seenBy =
                members.filter { message.seen_by.contains(it.user.id) && it.user.id != currentUserId }

            if (by != null) {
                MessageContent(
                    previousMessage = if (index > 0) messages[index - 1] else null,
                    nextMessage = if (index < messages.size - 1) messages[index + 1] else null,
                    message,
                    by = by,
                    seenBy = seenBy,
                    isGroupChat = members.size > 2,
                    isSender = currentUserId == message.sender_id
                )
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

@Composable
fun MessageContent(
    previousMessage: ApiThreadMessage?,
    nextMessage: ApiThreadMessage?,
    message: ApiThreadMessage,
    by: UserInfo?,
    seenBy: List<UserInfo>,
    isGroupChat: Boolean,
    isSender: Boolean
) {
    Row(
        Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = if (isSender) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        if (!isSender && by != null && isGroupChat) {
            UserProfile(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(50.dp),
                user = by.user
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        val timeLabel =
            if (isSender || by == null || !isGroupChat) message.formattedTime else "${by.user.first_name} • ${message.formattedTime}"

        val previousMsgHasSameSeenBy = previousMessage?.seen_by?.containsAll(message.seen_by) == false
        val seenLabel = if (isSender && seenBy.isNotEmpty() && !previousMsgHasSameSeenBy) {
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
            message = message.message, timeLabel = timeLabel,
            seenLabel = seenLabel, isSender = isSender
        )
    }
}

@Composable
fun MessageBubble(
    message: String, timeLabel: String,
    seenLabel: String,
    isSender: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val align = if (isSender) Alignment.End else Alignment.Start

    val shape = RoundedCornerShape(
        if (isSender) 16.dp else 0.dp,
        if (isSender) 0.dp else 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(max = screenWidth * 0.8f)
    ) {

        Text(
            text = timeLabel,
            style = AppTheme.appTypography.label3.copy(color = AppTheme.colorScheme.textDisabled),
            modifier = Modifier
                .padding(2.dp)
                .align(align)
        )

        Text(
            text = message,
            style = AppTheme.appTypography.body1.copy(color = AppTheme.colorScheme.textPrimary),
            modifier = Modifier
                .background(
                    color = if (isSender) {
                        AppTheme.colorScheme.primary.copy(alpha = 0.7f)
                    } else {
                        AppTheme.colorScheme.containerNormalOnSurface
                    },
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(align)

        )

        if (seenLabel.isNotEmpty())
            Text(
                text = seenLabel,
                style = AppTheme.appTypography.label3.copy(color = AppTheme.colorScheme.textDisabled),
                modifier = Modifier
                    .padding(2.dp).widthIn(max = screenWidth * 0.3f)
                    .align(align),
                overflow = TextOverflow.Ellipsis
            )
    }
}
