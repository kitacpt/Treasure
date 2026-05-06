package com.treasure.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.treasure.R

val Cormorant = FontFamily(
    // Variable fonts — single file per upright/italic axis covers all weights.
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.Medium),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_italic, FontWeight.Medium, FontStyle.Italic),
)

val SpaceGrotesk = FontFamily(
    // Variable font — single face supports the full weight axis.
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
)

val TreasureTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Cormorant,
        fontWeight = FontWeight.Medium,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Cormorant,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Cormorant,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Cormorant,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
    ),
    // Tabular numerals row (the three-tally on Portal): serif but tight
    headlineMedium = TextStyle(
        fontFamily = Cormorant,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
    ),
    // Mono small caps row — date strip, roman numeral corner
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.5.sp,
        letterSpacing = 1.7.sp,
    ),
)
