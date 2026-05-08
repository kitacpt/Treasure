package com.treasure.ui.add

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.treasure.core.domain.Category

/**
 * Tiny line-art icons for the four categories. Mirrors the prototype's
 * CatIcon — one shape per room, drawn with a single hairline stroke
 * to match the museum-plate aesthetic.
 */
@Composable
fun CategoryGlyph(
    category: Category,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = (w * 0.05f).coerceAtLeast(1.5f)
        val stroke = Stroke(
            width = sw,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (category) {
            Category.BADMINTON -> {
                // Racket: oval head + diagonal handle
                val headRx = w * 0.22f
                val headRy = h * 0.24f
                val cx = w * 0.42f
                val cy = h * 0.38f
                drawOval(
                    color = color,
                    topLeft = Offset(cx - headRx, cy - headRy),
                    size = Size(headRx * 2, headRy * 2),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(cx + headRx * 0.7f, cy + headRy * 0.7f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
            }
            Category.PHOTO -> {
                // Camera: body + lens circle + viewfinder bump
                val bodyTop = h * 0.30f
                val bodyHeight = h * 0.45f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, bodyTop),
                    size = Size(w * 0.76f, bodyHeight),
                    cornerRadius = CornerRadius(sw, sw),
                    style = stroke,
                )
                // viewfinder
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.40f, h * 0.20f),
                    size = Size(w * 0.20f, h * 0.10f),
                    cornerRadius = CornerRadius(sw * 0.4f, sw * 0.4f),
                    style = stroke,
                )
                // lens
                drawCircle(
                    color = color,
                    radius = h * 0.16f,
                    center = Offset(w * 0.50f, bodyTop + bodyHeight * 0.5f),
                    style = stroke,
                )
            }
            Category.CARS -> {
                // Sedan silhouette: body + roof + 2 wheels
                drawLine(
                    color = color,
                    start = Offset(w * 0.10f, h * 0.62f),
                    end = Offset(w * 0.90f, h * 0.62f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
                // body
                drawLine(
                    color = color,
                    start = Offset(w * 0.10f, h * 0.62f),
                    end = Offset(w * 0.22f, h * 0.45f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.45f),
                    end = Offset(w * 0.78f, h * 0.45f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.78f, h * 0.45f),
                    end = Offset(w * 0.90f, h * 0.62f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
                // wheels
                drawCircle(color = color, radius = w * 0.07f, center = Offset(w * 0.28f, h * 0.74f), style = stroke)
                drawCircle(color = color, radius = w * 0.07f, center = Offset(w * 0.72f, h * 0.74f), style = stroke)
            }
            Category.TECH -> {
                // Laptop: open clamshell
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.64f, h * 0.42f),
                    cornerRadius = CornerRadius(sw * 0.5f, sw * 0.5f),
                    style = stroke,
                )
                // base / hinge
                drawLine(
                    color = color,
                    start = Offset(w * 0.10f, h * 0.74f),
                    end = Offset(w * 0.90f, h * 0.74f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
