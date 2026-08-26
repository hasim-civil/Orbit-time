package com.hasim.orbittime.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii tokens. The reference design leans on large, soft rounding throughout. */
object OrbitRadius {
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 36.dp

    /** Fully rounded pill radius for buttons, chips and segmented toggles. */
    val pill = 999.dp
}

object OrbitShapes {
    val small = RoundedCornerShape(OrbitRadius.sm)
    val medium = RoundedCornerShape(OrbitRadius.md)
    val large = RoundedCornerShape(OrbitRadius.lg)
    val extraLarge = RoundedCornerShape(OrbitRadius.xl)
    val card = RoundedCornerShape(OrbitRadius.xxl)
    val hero = RoundedCornerShape(OrbitRadius.xxxl)
    val pill = RoundedCornerShape(OrbitRadius.pill)
}

val OrbitMaterialShapes = Shapes(
    extraSmall = OrbitShapes.small,
    small = OrbitShapes.medium,
    medium = OrbitShapes.large,
    large = OrbitShapes.card,
    extraLarge = OrbitShapes.hero,
)
