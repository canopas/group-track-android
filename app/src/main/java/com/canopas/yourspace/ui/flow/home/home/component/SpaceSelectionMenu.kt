package com.canopas.yourspace.ui.flow.home.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.flow.home.home.HomeScreenViewModel
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun SpaceSelectionMenu(modifier: Modifier, verifyingSpace: Boolean, isExpand: Boolean = false) {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    val selectedSpace = state.spaces.firstOrNull { it.space.id == state.selectedSpaceId }

    val alpha by animateFloatAsState(
        targetValue = if (isExpand) 2f else 1f,
        tween(durationMillis = 100),
        label = ""
    )

    val dropDownArrowRotation by
    animateFloatAsState(
        targetValue = if (state.showSpaceSelectionPopup) 180f else 0f,
        label = ""
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .graphicsLayer(alpha = alpha)
            .clip(CircleShape)
            .border(1.dp, color = AppTheme.colorScheme.outline, shape = CircleShape)
            .clickable { viewModel.toggleSpaceSelection() }
            .background(color = AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)

    ) {
        Text(
            text = selectedSpace?.space?.name
                ?: stringResource(id = R.string.home_space_selection_menu_default_text),
            style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
        )

        if (state.isLoadingSpaces || verifyingSpace) {
            CircularProgressIndicator(
                color = AppTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .height(16.dp)
                    .width(16.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "drop-down-arrow",
                modifier = Modifier.rotate(dropDownArrowRotation)
            )
        }
    }
}

@Composable
fun SpaceSelectionPopup(
    show: Boolean,
    spaces: List<SpaceInfo>,
    selectSpaceId: String?,
    onSpaceSelected: (String) -> Unit = {},
    onCreateSpace: () -> Unit = {},
    onJoinSpace: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = AppTheme.colorScheme.surface)
                .padding(16.dp)
                .padding(top = 60.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .animateContentSize()
            ) {
                items(spaces) { space ->
                    SpaceItem(
                        space,
                        space.space.id == selectSpaceId,
                        onSpaceSelected = onSpaceSelected
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 18.dp)) {
                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.common_btn_create_space),
                    onClick = onCreateSpace
                )
                Spacer(modifier = Modifier.width(16.dp))
                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.common_btn_join_space),
                    onClick = onJoinSpace
                )
            }
        }
    }
}

@Composable
private fun SpaceItem(
    space: SpaceInfo,
    isSelected: Boolean,
    onSpaceSelected: (String) -> Unit = {}
) {
    val admin = space.members.firstOrNull { it.user.id == space.space.admin_id }?.user
    val members = space.members
    val containerShape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .background(
                color = if (isSelected) AppTheme.colorScheme.containerNormal else AppTheme.colorScheme.containerB40,
                shape = containerShape
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AppTheme.colorScheme.primary else Color.Transparent,
                shape = containerShape
            )
            .clip(shape = containerShape)
            .clickable { onSpaceSelected(space.space.id) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = space.space.name,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = if (members.size > 1) {
                        R.string.home_space_selection_space_item_subtitle_members
                    } else {
                        R.string.home_space_selection_space_item_subtitle_member
                    },
                    admin?.fullName ?: stringResource(id = R.string.common_unknown),
                    members.size
                ),
                style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled)
            )
        }
    }
}
