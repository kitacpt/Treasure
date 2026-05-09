package com.treasure.illust

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min

/**
 * Stable palette wrapper: List<Color> is unstable to the Compose runtime, so
 * passing it directly forces every illustration to recompose on every parent
 * recomposition (no skipping, even if the colors didn't change). Wrapping it
 * in an @Immutable type lets Compose skip the Canvas / draw call when the
 * Item that owns the palette didn't change.
 */
@Immutable
@JvmInline
value class IllustPalette(val colors: List<Color>) {
    companion object {
        val Empty = IllustPalette(emptyList())
    }
}

/**
 * Shared utilities for the museum-plate illustrations. Each shape is drawn in
 * its own viewBox (matching the prototype's SVG coordinates exactly), then
 * letterboxed into the Canvas via [drawInViewBox] — the same behaviour as
 * SVG's default `preserveAspectRatio="xMidYMid meet"`.
 */

internal val INK = Color(0xFF1A1815)

internal fun parseHex(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or cleaned.toLong(16))
        8 -> Color(cleaned.toLong(16))
        else -> Color.Black
    }
}

internal inline fun DrawScope.drawInViewBox(
    vbW: Float,
    vbH: Float,
    block: DrawScope.() -> Unit,
) {
    val s = min(size.width / vbW, size.height / vbH)
    val tx = (size.width - vbW * s) / 2f
    val ty = (size.height - vbH * s) / 2f
    translate(tx, ty) {
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            block()
        }
    }
}

internal data class P4(val c0: Color, val c1: Color, val c2: Color, val c3: Color)

internal fun palette4(palette: IllustPalette, default: P4 = DefaultPalette): P4 = P4(
    palette.colors.getOrNull(0) ?: default.c0,
    palette.colors.getOrNull(1) ?: default.c1,
    palette.colors.getOrNull(2) ?: default.c2,
    palette.colors.getOrNull(3) ?: default.c3,
)

internal val DefaultPalette = P4(
    c0 = Color(0xFF1A1A1A),
    c1 = Color(0xFF3A3A3A),
    c2 = Color(0xFFD8D2C4),
    c3 = Color(0xFF7A7A7A),
)
