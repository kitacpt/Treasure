package com.treasure.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class TreasureColors(
    val paper: androidx.compose.ui.graphics.Color,
    val ink: androidx.compose.ui.graphics.Color,
    val terra: androidx.compose.ui.graphics.Color,
    val card: androidx.compose.ui.graphics.Color,
    val sub: androidx.compose.ui.graphics.Color,
    val line: androidx.compose.ui.graphics.Color,
)

val LocalTreasureColors = staticCompositionLocalOf {
    TreasureColors(Paper, Ink, Terra, Card, Sub, Line)
}

@Composable
fun TreasureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val treasureColors = if (darkTheme) {
        TreasureColors(PaperDark, InkDark, Terra, CardDark, SubDark, LineDark)
    } else {
        TreasureColors(Paper, Ink, Terra, Card, Sub, Line)
    }

    val materialColors = if (darkTheme) {
        darkColorScheme(
            background = treasureColors.paper,
            surface = treasureColors.paper,
            onBackground = treasureColors.ink,
            onSurface = treasureColors.ink,
            primary = treasureColors.terra,
        )
    } else {
        lightColorScheme(
            background = treasureColors.paper,
            surface = treasureColors.paper,
            onBackground = treasureColors.ink,
            onSurface = treasureColors.ink,
            primary = treasureColors.terra,
        )
    }

    CompositionLocalProvider(LocalTreasureColors provides treasureColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = TreasureTypography,
            content = content,
        )
    }
}
