package com.treasure.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn back chevron — heavier than a Unicode glyph so it reads as
 * a primary affordance without needing a label.
 */
@Composable
fun BackArrow(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .size(20.dp),
    ) {
        val w = size.width
        val h = size.height
        val sw = 2.6f.dp.toPx()
        val left = w * 0.18f
        val right = w * 0.85f
        val cy = h * 0.5f
        // arrow shaft
        drawLine(color, Offset(left, cy), Offset(right, cy), strokeWidth = sw, cap = StrokeCap.Round)
        // upper tick
        drawLine(color, Offset(left, cy), Offset(w * 0.45f, h * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
        // lower tick
        drawLine(color, Offset(left, cy), Offset(w * 0.45f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}
