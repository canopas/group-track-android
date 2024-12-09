package com.canopas.yourspace.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun NoMemberEmptyContent(
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
            modifier = Modifier.size(64.dp),
            tint = AppTheme.colorScheme.textPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_title),
            style = AppTheme.appTypography.header4
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_subtitle),
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.textDisabled,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.thread_screen_add_new_member),
            onClick = addMember,
            showLoader = loadingInviteCode
        )
    }
}
