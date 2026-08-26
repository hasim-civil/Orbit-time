package com.radiantengineering.orbittime.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Orbit Time colour tokens, pulled directly from the approved reference design.
 * Grouped by role rather than by screen so every phase draws from the same palette.
 */
object OrbitColors {

    // Cosmic / dark gradient stops — used for hero cards, punch-in surfaces, splash chrome.
    val void900 = Color(0xFF07040F)
    val void800 = Color(0xFF0A0710)
    val void700 = Color(0xFF0C0814)
    val void600 = Color(0xFF0D0820)
    val void500 = Color(0xFF180F31)
    val void400 = Color(0xFF1A0F31)
    val void300 = Color(0xFF2A1B4D)
    val void200 = Color(0xFF3D1F7A)
    val void100 = Color(0xFF4A2591)

    // Accent spectrum — the violet / blue / cyan / coral orbit gradient.
    val violet700 = Color(0xFF6D3BF5)
    val violet600 = Color(0xFF7C4DFF)
    val violet500 = Color(0xFFA084FF)
    val purple600 = Color(0xFF8B5CF6)
    val purple500 = Color(0xFFA855F7)
    val blue500 = Color(0xFF3B6DF5)
    val cyan400 = Color(0xFF38D6E8)
    val coral500 = Color(0xFFFF8A4C)
    val coral400 = Color(0xFFFF9A5C)

    // Light / cream surfaces — dashboard body, cards, welcome atmosphere base.
    val cream50 = Color(0xFFFFFDF9)
    val cream100 = Color(0xFFFFF8F1)
    val cream200 = Color(0xFFFBF6F2)
    val lilacWhite = Color(0xFFF7F3F8)
    val sand50 = Color(0xFFFDF6EC)
    val sand100 = Color(0xFFF9F2EA)
    val blush50 = Color(0xFFF6F0F3)
    val blush100 = Color(0xFFF5EEF1)
    val lavenderWhite = Color(0xFFF4F1FB)
    val mist = Color(0xFFF6F5F8)
    val fog = Color(0xFFF1EFF5)

    // Text.
    val ink900 = Color(0xFF14101D)
    val ink800 = Color(0xFF1B1424)
    val slate600 = Color(0xFF6B6479)
    val slate500 = Color(0xFF7C7589)
    val slate400 = Color(0xFF8A8394)
    val slate300 = Color(0xFFC0BACC)
    val slate200 = Color(0xFFC9C3D6)

    // Semantic status colours.
    val success = Color(0xFF34D19B)
    val successDark = Color(0xFF0F8A62)
    val successBg = Color(0xFFE6F7F0)
    val warning = Color(0xFFF0B34A)
    val warningDark = Color(0xFFE08A2F)
    val warningBg = Color(0xFFFDF0E6)
    val danger = Color(0xFFE2543F)
    val dangerBg = Color(0xFFFDECE7)
    val info = Color(0xFF3B6DF5)
    val infoBg = Color(0xFFE9EFFF)
    val accent = Color(0xFF7C4DFF)
    val accentBg = Color(0xFFEFE9FF)
}
