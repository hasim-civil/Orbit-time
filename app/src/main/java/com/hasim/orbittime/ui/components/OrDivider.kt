package com.hasim.orbittime.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/** A plain "or" divider between the primary action and an alternate sign-in method. */
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = OrbitColors.slate300)
        Text(
            text = "or",
            style = OrbitTypography.bodySmall,
            color = OrbitColors.slate500,
            modifier = Modifier.padding(horizontal = OrbitSpacing.md),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = OrbitColors.slate300)
    }
}
