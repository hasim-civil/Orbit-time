package com.radiantengineering.orbittime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.radiantengineering.orbittime.ui.theme.OrbitColors
import com.radiantengineering.orbittime.ui.theme.OrbitTypography

private val OrbitButtonHeight = 56.dp
private val OrbitButtonShape: Shape = CircleShape

/** Primary call-to-action: the blue → violet → coral gradient pill (e.g. "Sign in"). */
@Composable
fun OrbitGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OrbitButtonHeight)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        OrbitColors.blue500,
                        OrbitColors.purple500,
                        OrbitColors.coral500,
                    ),
                ),
                shape = OrbitButtonShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = OrbitTypography.buttonLabel,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/** Secondary call-to-action: a plain elevated pill on the cream surface (e.g. "Create account"). */
@Composable
fun OrbitOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(OrbitButtonHeight),
        shape = OrbitButtonShape,
        color = OrbitColors.cream50,
        contentColor = OrbitColors.ink900,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.wrapContentHeight()) {
            Text(
                text = text,
                style = OrbitTypography.buttonLabel,
                color = OrbitColors.ink900,
                textAlign = TextAlign.Center,
            )
        }
    }
}
