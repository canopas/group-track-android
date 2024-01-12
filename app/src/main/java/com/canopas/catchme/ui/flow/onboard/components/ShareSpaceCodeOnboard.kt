package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.component.PrimaryTextButton
import com.canopas.catchme.ui.theme.AppTheme

//UGBFMJ
@Composable
fun ShareSpaceCodeOnboard(
    spaceCode: String, shareCode: () -> Unit,
    onDoneSharing: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_share_space_code_title),
            style = AppTheme.appTypography.header1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.onboard_share_space_code_subtitle),
            style = AppTheme.appTypography.header4.copy(fontWeight = FontWeight.W500),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        InvitationCodeComponent(spaceCode)
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_share_space_code_tips),
            style = AppTheme.appTypography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            label = stringResource(R.string.onboard_share_space_code_btn),
            onClick = shareCode
        )
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryTextButton(
            label = stringResource(R.string.common_btn_skip),
            onClick = onDoneSharing
        )
    }
}

@Composable
fun InvitationCodeComponent(spaceCode: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(color = AppTheme.colorScheme.tertiary, shape = RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = spaceCode,
            style = AppTheme.appTypography.header1.copy(
                color = AppTheme.colorScheme.textInversePrimary,
                fontSize = 34.sp, letterSpacing = 0.8.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboard_share_code_expiry_desc),
            style = AppTheme.appTypography.body1.copy(
                color = AppTheme.colorScheme.textInversePrimary.copy(
                    alpha = 0.8f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )

    }
}
