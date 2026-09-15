package com.hasim.orbittime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/** Matches the app's other modals: 28dp cream card, 22dp padding, soft shadow. */
private val UpdateCardShape = OrbitShapes.card
private val NotesMaxHeight = 190.dp

/**
 * The "New update available" modal.
 *
 * Built from the same tokens as the rest of Orbit Time — cream card, Instrument Serif headline,
 * Manrope body, the blue→violet→coral gradient on the primary action — rather than a stock
 * Material dialog, so it reads as part of the app instead of a system prompt.
 *
 * While the APK is downloading the dialog stays put and turns into a progress view: the buttons
 * are replaced by the bar, and the card can no longer be dismissed by tapping outside, so a
 * stray tap can't cancel a transfer that's nearly done. "Later" remains available and cancels.
 */
@Composable
fun OrbitUpdateDialog(
    versionLabel: String,
    currentVersionLabel: String?,
    releaseNotes: String,
    isDownloading: Boolean,
    progress: Float?,
    errorMessage: String?,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isDownloading) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrbitColors.cream50, UpdateCardShape)
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpdateGlyph()
                Spacer(modifier = Modifier.size(OrbitSpacing.md))
                Column {
                    Text(
                        text = "ORBIT TIME",
                        style = OrbitTypography.label,
                        color = OrbitColors.violet600,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                    Text(
                        text = "New update available",
                        style = OrbitTypography.headline,
                        color = OrbitColors.ink900,
                    )
                }
            }

            Spacer(modifier = Modifier.height(OrbitSpacing.lg))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            ) {
                VersionPill(label = versionLabel)
                if (currentVersionLabel != null) {
                    Text(
                        text = "You have $currentVersionLabel",
                        style = OrbitTypography.bodySmall,
                        color = OrbitColors.slate500,
                    )
                }
            }

            if (releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                Text(text = "WHAT'S NEW", style = OrbitTypography.label, color = OrbitColors.slate500)
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = NotesMaxHeight)
                        .background(OrbitColors.mist, OrbitShapes.medium)
                        .padding(OrbitSpacing.md),
                ) {
                    Text(
                        text = releaseNotes,
                        style = OrbitTypography.bodyMedium,
                        color = OrbitColors.slate600,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(OrbitSpacing.md))
                Text(
                    text = errorMessage,
                    style = OrbitTypography.bodySmall,
                    color = OrbitColors.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrbitColors.dangerBg, OrbitShapes.small)
                        .padding(OrbitSpacing.md),
                )
            }

            Spacer(modifier = Modifier.height(OrbitSpacing.xl))

            if (isDownloading) {
                // An unknown length (the server sent no Content-Length) shows the indeterminate
                // bar rather than a progress value that would be a guess.
                if (progress == null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = OrbitColors.violet600,
                        trackColor = OrbitColors.fog,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = OrbitColors.violet600,
                        trackColor = OrbitColors.fog,
                    )
                }
                Spacer(modifier = Modifier.height(OrbitSpacing.md))
                Text(
                    text = if (progress == null) "Downloading…" else "Downloading… ${(progress * 100).toInt()}%",
                    style = OrbitTypography.bodySmall,
                    color = OrbitColors.slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.md))
                DialogTextAction(label = "Later", onClick = onLater)
            } else {
                OrbitGradientButton(text = "Update now", onClick = onUpdateNow)
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                DialogTextAction(label = "Later", onClick = onLater)
            }
        }
    }
}

/** The version being offered, in the app's accent violet. */
@Composable
private fun VersionPill(label: String) {
    Box(
        modifier = Modifier
            .background(OrbitColors.accentBg, OrbitShapes.pill)
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.xs),
    ) {
        Text(text = label, style = OrbitTypography.titleMedium, color = OrbitColors.violet600)
    }
}

/** A quiet full-width secondary action, matching the modals' Cancel row. */
@Composable
private fun DialogTextAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(OrbitColors.mist, OrbitShapes.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OrbitTypography.titleMedium,
            color = OrbitColors.slate600,
            textAlign = TextAlign.Center,
        )
    }
}

/** The gradient orbit mark used as the dialog's icon, drawn from the palette rather than an asset. */
@Composable
private fun UpdateGlyph() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrbitColors.blue500, OrbitColors.purple500, OrbitColors.coral500),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "↓", style = OrbitTypography.titleLarge, color = OrbitColors.cream50)
    }
}
