package com.canopas.yourspace.ui.flow.messages.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun MemberList(
    spaceName: String,
    userInfos: List<UserInfo>,
    selectedMember: List<UserInfo>,
    selectAll: Boolean = true,
    onMemberSelect: (UserInfo) -> Unit,
    selectAllMember: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colorScheme.containerLow),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            AllMemberItem(spaceName, selectAll, selectAllMember)
        }
        items(userInfos) { user ->
            MemberItem(
                user,
                selectedMember.any { it == user } && !selectAll,
                onMemberSelect
            )
        }
    }
}

@Composable
fun MemberItem(userInfo: UserInfo, isSelected: Boolean, onMemberSelect: (UserInfo) -> Unit) {
    val user = userInfo.user
    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable { onMemberSelect(userInfo) }
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            UserProfile(Modifier.fillMaxSize(), user = user)

            if (isSelected) {
                SelectionOverlay()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = user.first_name ?: "",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textPrimary)
        )
    }
}

@Composable
private fun SelectionOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.successColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = AppTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun AllMemberItem(
    spaceName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderWidth = if (isSelected) 0.dp else 1.dp
    Column(
        modifier = Modifier.width(56.dp)
            .clickable { onClick() }
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(borderWidth, AppTheme.colorScheme.primary, CircleShape)
                .background(AppTheme.colorScheme.containerLow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.messages_member_all),
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.primary)
            )
            if (isSelected) {
                SelectionOverlay()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = spaceName,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textPrimary)
        )
    }
}
