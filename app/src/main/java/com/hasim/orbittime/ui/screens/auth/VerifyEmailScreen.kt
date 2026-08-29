package com.hasim.orbittime.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/**
 * Gate screen shown while a signed-in Firebase user's email is not yet verified — reached after
 * registration, or after signing in to an account that was never verified. Blocks the rest of
 * the app (there's no way forward from here except verifying or signing out) using purely
 * Firebase Authentication's own built-in flow: `sendEmailVerification()` / `reload()` /
 * `isEmailVerified`, never a custom OTP.
 */
@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: VerifyEmailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    VerifyEmailContent(
        uiState = uiState,
        onCheckVerificationClick = { viewModel.checkVerification(onVerified) },
        onResendClick = viewModel::resendVerificationEmail,
        onSignOutClick = {
            viewModel.signOut()
            onSignedOut()
        },
    )
}

@Composable
fun VerifyEmailContent(
    uiState: VerifyEmailUiState,
    onCheckVerificationClick: () -> Unit,
    onResendClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(OrbitSpacing.giant))

            EnvelopeBadge()

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

            Text(
                text = "Verify your email",
                style = OrbitTypography.displayLarge,
                color = OrbitColors.ink900,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.sm))

            if (uiState.email.isNotBlank()) {
                Text(
                    text = uiState.email,
                    style = OrbitTypography.titleMedium,
                    color = OrbitColors.violet600,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.md))
            }

            Text(
                text = "We've sent a verification link to your email. Verify your email to continue.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                Text(
                    text = uiState.errorMessage,
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.danger,
                    textAlign = TextAlign.Center,
                )
            } else if (uiState.infoMessage != null) {
                Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                Text(
                    text = uiState.infoMessage,
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.success,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

            if (uiState.isChecking) {
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = OrbitColors.violet600)
                }
            } else {
                OrbitGradientButton(text = "Check verification", onClick = onCheckVerificationClick)
            }

            Spacer(modifier = Modifier.height(OrbitSpacing.md))

            val resendLabel = when {
                uiState.isResending -> "Sending…"
                uiState.cooldownSecondsRemaining > 0 -> "Resend email (${uiState.cooldownSecondsRemaining}s)"
                else -> "Resend email"
            }
            OrbitOutlineButton(
                text = resendLabel,
                onClick = onResendClick,
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

            Text(
                text = "Sign out",
                style = OrbitTypography.titleMedium,
                color = OrbitColors.slate600,
                modifier = Modifier.clickable(onClick = onSignOutClick),
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
        }
    }
}

/** A soft glass, violet-tinted circular badge holding a hand-drawn envelope-with-checkmark
 * glyph — matches this app's convention of drawing its own small icons on a Canvas (see e.g.
 * Profile's PersonIcon/CalendarIcon) rather than importing an illustration asset. */
@Composable
private fun EnvelopeBadge() {
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(OrbitColors.accentBg, OrbitColors.accentBg.copy(alpha = 0.4f)),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(52.dp)) {
            val scale = size.minDimension / 24f
            fun px(v: Float) = v * scale

            drawRoundRect(
                color = OrbitColors.violet600,
                topLeft = Offset(px(2f), px(5f)),
                size = androidx.compose.ui.geometry.Size(px(20f), px(14f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(px(2.5f), px(2.5f)),
                style = Stroke(width = px(1.7f)),
            )
            val flap = Path().apply {
                moveTo(px(2.5f), px(6f))
                lineTo(px(12f), px(14f))
                lineTo(px(21.5f), px(6f))
            }
            drawPath(flap, color = OrbitColors.violet600, style = Stroke(width = px(1.7f)))

            // Small check-badge peeking over the envelope's bottom-right corner.
            drawCircle(color = OrbitColors.success, radius = px(4.6f), center = Offset(px(19.5f), px(19.5f)))
            val check = Path().apply {
                moveTo(px(17.4f), px(19.6f))
                lineTo(px(18.9f), px(21.1f))
                lineTo(px(21.7f), px(17.9f))
            }
            drawPath(check, color = Color.White, style = Stroke(width = px(1.6f)))
        }
    }
}
