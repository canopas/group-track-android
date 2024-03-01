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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun ColumnScope.MessageList(
    loading: Boolean,
    messages: LazyPagingItems<ApiThreadMessage>,
    members: List<UserInfo>,
    currentUserId: String
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        reverseLayout = true
    ) {
        items(messages.itemCount, key = { messages[it]!!.id }) { index ->
            val message = messages[index]

            val by =
                members.firstOrNull { message != null && it.user.id == message.sender_id }

            if (by != null && message != null) {
                MessageContent(
                    message, by = by, showProfile = members.size > 2,
                    isSender = currentUserId == message.sender_id
                )
            }
        }

        if (messages.loadState.append == LoadState.Loading && !loading) {
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
    message: ApiThreadMessage,
    by: UserInfo?,
    showProfile: Boolean,
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
        },
        verticalAlignment = androidx.compose.ui.Alignment.Bottom
    ) {
        if (!isSender && by != null && showProfile) {
            UserProfile(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(50.dp),
                user = by.user
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        MessageBubble(message = message.message, time = message.formattedTime, isSender = isSender)
    }
}

@Composable
fun MessageBubble(message: String, time: String, isSender: Boolean) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        if (isSender) 0.dp else 20.dp,
        if (isSender) 20.dp else 0.dp
    )
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(max = screenWidth * 0.8f)
    ) {
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
        )

        Text(
            text = time,
            style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textDisabled),
            modifier = Modifier
                .padding(2.dp)
                .align(
                    if (isSender) {
                        Alignment.End
                    } else {
                        androidx.compose.ui.Alignment.Start
                    }
                )
        )
    }
}