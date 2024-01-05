package com.canopas.catchme.ui.flow.intro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.PagerIndicator
import com.canopas.catchme.ui.theme.AppTheme.colorScheme
import com.canopas.catchme.ui.theme.AppTypography
import com.canopas.catchme.ui.theme.KalamBoldFont

@Composable
fun IntroView() {
    Box(modifier = Modifier.fillMaxSize()) {
        IntroBackground()
        IntroContent()
    }
}

@Composable
fun IntroBackground(){
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
fun IntroContent() {
    val pagerState = rememberPagerState {
        introList.size
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        Spacer(modifier = Modifier.padding(top = 80.dp))

        Image(
            painter = painterResource(id = R.drawable.app_logo_white_outlined),
            contentDescription = "app_log",
            modifier = Modifier.size(50.dp)
        )

        Text(
            text = "CatchMe",
            textAlign = TextAlign.Center,
            style = AppTypography.headlineLarge
                .copy(
                    color = colorScheme.surface,
                    fontFamily = KalamBoldFont
                ),
        )

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
fun GetStartedButton() {
    Button(
        onClick = { },
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
            style = AppTypography.labelMedium,
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
            style = AppTypography.headlineMedium.copy(color = colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        )
    }


}

@Preview
@Composable
fun IntroPreview() {
    IntroView()
}

val introList = listOf(
    R.string.intro_content_one,
    R.string.intro_content_two,
    R.string.intro_content_three
)