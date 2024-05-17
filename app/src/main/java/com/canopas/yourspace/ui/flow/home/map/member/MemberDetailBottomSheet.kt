package com.canopas.yourspace.ui.flow.home.map.member

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.flow.home.map.member.components.LocationHistory
import com.canopas.yourspace.ui.theme.AppTheme
import java.util.Calendar

@Composable
fun MemberDetailBottomSheetContent(
    userInfo: UserInfo
) {
    val viewModel = hiltViewModel<MemberDetailViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userInfo) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val timestamp = calendar.timeInMillis
        viewModel.fetchUserLocationHistory(
            from = timestamp,
            to = System.currentTimeMillis(),
            userInfo
        )
    }

    Column(modifier = Modifier.fillMaxHeight(0.9f)) {
        UserInfoContent(userInfo)
        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 1.dp, color = AppTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.member_detail_location_history_today),
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LocationHistory(state.isLoading, state.locations, viewModel::loadMoreLocations, viewModel::addPlace, viewModel::showJourneyDetails)
    }
}

@Composable
private fun UserInfoContent(userInfo: UserInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserProfile(modifier = Modifier.size(40.dp), user = userInfo.user)
            Column(
                modifier = Modifier
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userInfo.user.fullName,
                    style = AppTheme.appTypography.header3,
                    maxLines = 1
                )
                if (!userInfo.isLocationEnable) {
                    Text(
                        text = stringResource(id = R.string.map_user_item_location_off),
                        style = AppTheme.appTypography.label1.copy(
                            color = Color.Red,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }

//        IconButton(
//            modifier = Modifier
//                .size(40.dp)
//                .background(
//                    color = AppTheme.colorScheme.primary,
//                    shape = CircleShape
//                ),
//            onClick = {
//                viewModel.navigateToUserJourneyDetail()
//            }
//        ) {
//            Icon(
//                painterResource(id = R.drawable.ic_location_journey),
//                contentDescription = "",
//                tint = AppTheme.colorScheme.surface,
//                modifier = Modifier
//                    .size(36.dp)
//                    .padding(4.dp)
//            )
//        }
    }
}
