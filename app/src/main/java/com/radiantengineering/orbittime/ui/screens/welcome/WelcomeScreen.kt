package com.radiantengineering.orbittime.ui.screens.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

/** Reference-width orbit symbol diameter, at the reference screen width of 393dp. */
private val ReferenceOrbSize = 220.dp
private val ReferenceScreenWidth = 393.dp
private val MaxContentWidth = 480.dp
private val CompactHeightThreshold = 640.dp

/**
 * The Welcome screen: the first thing anyone sees. Purely presentational —
 * navigation is delegated to the two callbacks so this phase has no auth wiring.
 * Scales the orbit symbol and, on unusually short viewports, switches to a
 * scrollable layout so nothing clips — the reference proportions are otherwise untouched.
 */
@Composable
fun WelcomeScreen(
    strings: WelcomeScreenStrings,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        val orbSize = (maxWidth * (ReferenceOrbSize / ReferenceScreenWidth)).coerceIn(160.dp, 240.dp)
        val isCompactHeight = maxHeight < CompactHeightThreshold

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (isCompactHeight) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = OrbitSpacing.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                    OrbitSymbol(diameter = orbSize)
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                    WelcomeTitleBlock(title = strings.title, tagline = strings.tagline)
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxxl))
                    WelcomeButtonsBlock(
                        signIn = strings.signIn,
                        createAccount = strings.createAccount,
                        onSignInClick = onSignInClick,
                        onCreateAccountClick = onCreateAccountClick,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = OrbitSpacing.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.weight(0.9f))
                    OrbitSymbol(diameter = orbSize)
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                    WelcomeTitleBlock(title = strings.title, tagline = strings.tagline)
                    Spacer(modifier = Modifier.weight(1.1f))
                    WelcomeButtonsBlock(
                        signIn = strings.signIn,
                        createAccount = strings.createAccount,
                        onSignInClick = onSignInClick,
                        onCreateAccountClick = onCreateAccountClick,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun WelcomeTitleBlock(title: String, tagline: String) {
    Text(
        text = title,
        style = OrbitTypography.displayHero,
        color = OrbitColors.ink900,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(OrbitSpacing.sm))
    Text(
        text = tagline,
        style = OrbitTypography.bodyLarge,
        color = OrbitColors.slate600,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomeButtonsBlock(
    signIn: String,
    createAccount: String,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    OrbitGradientButton(text = signIn, onClick = onSignInClick)
    Spacer(modifier = Modifier.height(OrbitSpacing.md))
    OrbitOutlineButton(text = createAccount, onClick = onCreateAccountClick)
}

@Composable
fun rememberWelcomeScreenStrings(): WelcomeScreenStrings = WelcomeScreenStrings(
    title = stringResource(R.string.app_name),
    tagline = stringResource(R.string.welcome_tagline),
    signIn = stringResource(R.string.welcome_sign_in),
    createAccount = stringResource(R.string.welcome_create_account),
)

@Preview(name = "Reference phone", showBackground = true, widthDp = 393, heightDp = 852)
@Preview(name = "Compact / landscape", showBackground = true, widthDp = 780, heightDp = 360)
@Preview(name = "Tablet", showBackground = true, widthDp = 800, heightDp = 1000)
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
