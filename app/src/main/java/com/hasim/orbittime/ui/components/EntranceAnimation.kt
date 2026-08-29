package com.hasim.orbittime.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val CardRiseEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The reference's "cardRise" entrance: a one-shot 14dp slide-up + fade-in, played once when the
 * modified composable enters composition — e.g. a tab's content on first show, doubling as this
 * app's page-content transition since every screen already applies it to its own content column.
 * 260ms keeps it in the "subtle, ~200-300ms" range for a tab switch rather than a slower one-off
 * card reveal.
 */
@Composable
fun Modifier.cardRiseEntrance(): Modifier {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(260, easing = CardRiseEasing))
    }
    val offsetPx = with(density) { 14.dp.toPx() }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}
