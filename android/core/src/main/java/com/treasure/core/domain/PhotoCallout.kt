package com.treasure.core.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 单条照片标注：用户在全屏看图时长按某个位置加一行文字。
 * 坐标是相对照片的归一化值 (0..1)，与显示尺寸无关。
 */
@Immutable
@Serializable
data class PhotoCallout(
    val x: Float,
    val y: Float,
    val text: String,
)
