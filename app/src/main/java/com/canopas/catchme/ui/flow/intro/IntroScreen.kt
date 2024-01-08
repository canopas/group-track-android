package com.canopas.catchme.ui.flow.intro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.AppLogo
import com.canopas.catchme.ui.component.PagerIndicator
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.AppTheme.colorScheme


@Composable
fun IntroScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        IntroBackground()
        IntroContent()
    }
}

@Composable
private fun IntroBackground() {
    Image(
        painter = painterResource(id = R.drawable.bg_intro),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "background"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.6f))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntroContent() {
    val pagerState = rememberPagerState {
        introList.size
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        Spacer(modifier = Modifier.padding(top = 80.dp))
        AppLogo()

        TagLine()

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            IntroItem(stringResource(id = introList[page]))
        }

        Spacer(modifier = Modifier.padding(top = 28.dp))


        PagerIndicator(
            pagerState = pagerState,
            count = introList.size,
            activeColor = colorScheme.primary,
            inactiveColor = colorScheme.containerInverseHigh,
            activeIndicatorWidth = 16.dp,
            spacing = 6.dp,
        )

        Spacer(modifier = Modifier.padding(top = 28.dp))

        GetStartedButton()

        Spacer(modifier = Modifier.padding(bottom = 80.dp))
    }
}

@Composable
fun TagLine() {
    Divider(
        modifier = Modifier
            .width(20.dp)
            .background(colorScheme.outline.copy(alpha = 0.5f))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Stay Close, Anywhere!",
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.subTitle2
            .copy(
                color = colorScheme.surface,
                letterSpacing = -(0.56).sp
            ),
    )
}

@Composable
fun GetStartedButton() {
    val viewModel = hiltViewModel<IntroViewModel>()
    Button(
        onClick = { viewModel.completedIntro() },
        modifier = Modifier
            .fillMaxWidth(fraction = 0.7f)
            .padding(horizontal = 40.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
        )
    ) {
        Text(
            text = stringResource(id = R.string.get_started),
            style = AppTheme.appTypography.label1.copy(color = colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )

    }
}

@Composable
fun IntroItem(content: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = content,
            textAlign = TextAlign.Center,
            style = AppTheme.appTypography.header3.copy(color = colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        )
    }


}

@Preview
@Composable
private fun IntroPreview() {
    IntroScreen()
}

private val introList = listOf(
    R.string.intro_content_one,
    R.string.intro_content_two,
    R.string.intro_content_three
)