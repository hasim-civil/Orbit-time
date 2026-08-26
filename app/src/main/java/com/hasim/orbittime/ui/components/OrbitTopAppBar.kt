package com.hasim.orbittime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/** The logo + wordmark + bell + avatar bar shown atop every authenticated screen. */
@Composable
fun OrbitTopAppBar(
    userInitials: String,
    hasNotification: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitSpacing.screenHorizontal, vertical = OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(OrbitColors.void300, OrbitColors.void600, OrbitColors.void900),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            OrbitMiniLogo()
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        Text(
            text = "ORBIT TIME",
            style = OrbitTypography.label,
            color = OrbitColors.ink900,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(OrbitColors.cream50, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val bell = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.06f)
                    cubicTo(
                        size.width * 0.22f, size.height * 0.1f,
                        size.width * 0.2f, size.height * 0.4f,
                        size.width * 0.2f, size.height * 0.55f,
                    )
                    lineTo(size.width * 0.1f, size.height * 0.75f)
                    lineTo(size.width * 0.9f, size.height * 0.75f)
                    lineTo(size.width * 0.8f, size.height * 0.55f)
                    cubicTo(
                        size.width * 0.8f, size.height * 0.4f,
                        size.width * 0.78f, size.height * 0.1f,
                        size.width * 0.5f, size.height * 0.06f,
                    )
                    close()
                }
                drawPath(bell, color = OrbitColors.ink900)
                drawOval(
                    color = OrbitColors.ink900,
                    topLeft = Offset(size.width * 0.4f, size.height * 0.8f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.14f),
                )
            }
            if (hasNotification) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(OrbitColors.danger, CircleShape)
                        .align(Alignment.TopEnd),
                )
            }
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(OrbitColors.violet600, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = userInitials, style = OrbitTypography.bodySmall, color = OrbitColors.cream50)
        }
    }
}
