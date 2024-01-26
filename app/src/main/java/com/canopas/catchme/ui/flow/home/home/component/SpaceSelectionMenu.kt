package com.canopas.catchme.ui.flow.home.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults.smallShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.data.models.space.SpaceInfo
import com.canopas.catchme.ui.component.AppProgressIndicator
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.flow.home.home.HomeScreenViewModel
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun SpaceSelectionMenu(modifier: Modifier) {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    val selectedSpace = state.spaces.firstOrNull { it.space.id == state.selectedSpaceId }

    FloatingActionButton(
        { viewModel.toggleSpaceSelection() },
        modifier = modifier
            .padding(6.dp)
            .height(40.dp),
        containerColor = AppTheme.colorScheme.surface,
        contentColor = AppTheme.colorScheme.primary,
        shape = smallShape
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 20.dp, end = 10.dp)
        ) {
            Text(
                text = selectedSpace?.space?.name
                    ?: stringResource(id = R.string.home_space_selection_menu_default_text),
                style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textPrimary)
            )

            if (state.isLoadingSpaces) {
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
                    contentDescription = "drop-down-arrow"
                )
            }
        }
    }
}

@Composable
fun SpaceSelectionPopup(
    show: Boolean, spaces: List<SpaceInfo>, selectSpaceId: String,
    onSpaceSelected: (String) -> Unit = {},
    onCreateSpace: () -> Unit = {}, onJoinSpace: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    10.dp,
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                    clip = false
                )
                .background(
                    color = AppTheme.colorScheme.surface,
                    shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                )
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

            Row(modifier = Modifier.padding(top = 20.dp)) {
                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.common_btn_create_space),
                    onClick = onCreateSpace
                )
                Spacer(modifier = Modifier.width(10.dp))
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
    val admin = space.members.first { it.user.id == space.space.admin_id }.user
    val members = space.members
    val containerShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .background(
                color = if (isSelected) AppTheme.colorScheme.primary.copy(0.1f) else AppTheme.colorScheme.containerNormal,
                shape = containerShape
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AppTheme.colorScheme.primary else Color.Transparent,
                shape = containerShape
            )
            .clip(shape = containerShape)
            .clickable { onSpaceSelected(space.space.id) }
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {

            Text(
                text = space.space.name,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )

            Text(
                text = stringResource(
                    id = if (members.size > 1) R.string.home_space_selection_space_item_subtitle_members
                    else R.string.home_space_selection_space_item_subtitle_member,
                    admin.fullName,
                    members.size
                ),
                style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textSecondary)
            )
        }

    }
}
