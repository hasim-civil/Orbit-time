package com.radiantengineering.orbittime.ui.screens.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.radiantengineering.orbittime.R
import com.radiantengineering.orbittime.ui.components.OrbitGradientButton
import com.radiantengineering.orbittime.ui.components.OrbitOutlineButton
import com.radiantengineering.orbittime.ui.theme.OrbitColors
import com.radiantengineering.orbittime.ui.theme.OrbitSpacing
import com.radiantengineering.orbittime.ui.theme.OrbitTimeTheme
import com.radiantengineering.orbittime.ui.theme.OrbitTypography

/** Copy for [WelcomeScreen], hoisted so the screen itself has no resource lookups. */
data class WelcomeScreenStrings(
    val title: String,
    val tagline: String,
    val signIn: String,
    val createAccount: String,
)

/**
 * The Welcome screen: the first thing anyone sees. Purely presentational —
 * navigation is delegated to the two callbacks so this phase has no auth wiring.
 */
@Composable
fun WelcomeScreen(
    strings: WelcomeScreenStrings,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = OrbitSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.9f))

            OrbitSymbol()

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

            Text(
                text = strings.title,
                style = OrbitTypography.displayHero,
                color = OrbitColors.ink900,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.sm))

            Text(
                text = strings.tagline,
                style = OrbitTypography.bodyLarge,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1.1f))

            OrbitGradientButton(
                text = strings.signIn,
                onClick = onSignInClick,
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.md))

            OrbitOutlineButton(
                text = strings.createAccount,
                onClick = onCreateAccountClick,
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
        }
    }
}

@Composable
fun rememberWelcomeScreenStrings(): WelcomeScreenStrings = WelcomeScreenStrings(
    title = stringResource(R.string.app_name),
    tagline = stringResource(R.string.welcome_tagline),
    signIn = stringResource(R.string.welcome_sign_in),
    createAccount = stringResource(R.string.welcome_create_account),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WelcomeScreenPreview() {
    OrbitTimeTheme {
        WelcomeScreen(
            strings = rememberWelcomeScreenStrings(),
            onSignInClick = {},
            onCreateAccountClick = {},
        )
    }
}
