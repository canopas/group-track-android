package com.canopas.catchme.ui.flow.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.AppLogo
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.CatchMeTheme

@Composable
fun SignInScreen() {
    Scaffold(
        topBar = {
            SignInAppBar()
        }
    ) {
        SignInContent(
            modifier = Modifier
                .padding(it)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInAppBar(){
    TopAppBar(title = { },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            TextButton(onClick = {}) {
                Text(
                    text = stringResource(
                        id = R.string.sign_in_skip
                    ),
                    style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textPrimary)
                )
            }
        })
}

@Composable
private fun SignInContent(modifier: Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppTheme.colorScheme.primary.copy(0f),
                        AppTheme.colorScheme.primary.copy(0.12F),
                        AppTheme.colorScheme.primary.copy(0f)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        AppLogo(colorTint = AppTheme.colorScheme.primary)
        Spacer(modifier = Modifier.weight(1f))
        GoogleSignInBtn(onClick = {})
        Spacer(modifier = Modifier.height(20.dp))
        PhoneLoginBtn(onClick = {})
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PhoneLoginBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary,
        )
    ) {
        Text(
            text = stringResource(id = R.string.sign_in_btn_continue_with_phone),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )

    }
}

@Composable
private fun GoogleSignInBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_sign_in_google_logo),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(id = R.string.sign_in_btn_continue_with_google),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}

@Preview
@Composable
fun PreviewSignInView() {
    CatchMeTheme {
        SignInScreen()
    }
}