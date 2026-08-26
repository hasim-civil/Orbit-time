package com.hasim.orbittime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/** A small tinted status strip for offline/error messaging, shared by the Home and Punch screens. */
@Composable
fun InlineBanner(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(OrbitSpacing.md))
            .padding(horizontal = OrbitSpacing.lg, vertical = OrbitSpacing.md),
    ) {
        Text(text = text, style = OrbitTypography.bodyMedium, color = color)
    }
}
