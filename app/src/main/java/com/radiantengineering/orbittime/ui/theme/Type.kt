package com.radiantengineering.orbittime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.radiantengineering.orbittime.R

/**
 * The two brand typefaces from the reference design:
 * Instrument Serif for display moments, Manrope for everything else.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Italic),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Named brand text styles, used directly where the Material3 slot names don't fit. */
object OrbitTypography {
    val displayHero = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    )
    val displayLarge = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    )
    val displayMedium = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    )
    val headline = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )
    val titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val label = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
    )
    val buttonLabel = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
    )
}

/** Material3 typography, mapped onto the same brand styles so default components stay on-brand. */
val OrbitMaterialTypography = Typography(
    displayLarge = OrbitTypography.displayHero,
    displayMedium = OrbitTypography.displayLarge,
    displaySmall = OrbitTypography.displayMedium,
    headlineLarge = OrbitTypography.displayMedium,
    headlineMedium = OrbitTypography.headline,
    headlineSmall = OrbitTypography.headline,
    titleLarge = OrbitTypography.titleLarge,
    titleMedium = OrbitTypography.titleMedium,
    titleSmall = OrbitTypography.bodySmall,
    bodyLarge = OrbitTypography.bodyLarge,
    bodyMedium = OrbitTypography.bodyMedium,
    bodySmall = OrbitTypography.bodySmall,
    labelLarge = OrbitTypography.buttonLabel,
    labelMedium = OrbitTypography.label,
    labelSmall = OrbitTypography.label,
)
