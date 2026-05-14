package com.treasure.ui.add

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.treasure.audio.VoiceRecorder
import com.treasure.theme.LocalTreasureColors
import kotlinx.coroutines.delay

/**
 * Cycle 0034 v2：长按麦克风进来的全屏录音页。
 *
 *  - 顶部 [取消] / "正在录音…" / 计时秒数
 *  - 中央大圆 + 麦克风 + 音量脉冲（按 maxAmplitude 实时缩放）
 *  - 底部两颗大圆按钮：[✕ 取消] / [✓ 发送]。手势上按用户要求是"松手发送"，
 *    但我们额外保留点击按钮，跟语义对得上更顺手。
 *
 * 调用方传 [recorder]（已 start()），传 [onCancel] / [onSend]（传录好的 path
 * + duration）。本组件不去 start recorder，避免与 permission 流程耦合。
 */
@Composable
fun RecordingOverlay(
    recorder: VoiceRecorder,
    onCancel: () -> Unit,
    onSend: (path: String, duration: String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var elapsedSec by remember { mutableStateOf(0) }
    var amplitude by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (recorder.isRecording) {
            elapsedSec = (recorder.elapsedMs() / 1000).toInt()
            amplitude = recorder.currentAmplitude()
            delay(60)
        }
    }

    fun doSend() {
        val result = recorder.stop() ?: run {
            onCancel()
            return
        }
        onSend(result.first.absolutePath, result.second)
    }
    fun doCancel() {
        recorder.cancel()
        onCancel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "取消",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { doCancel() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "正在录音",
                    color = colors.terra,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.width(60.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MicPulse(amplitude = amplitude, color = colors.terra, paperColor = colors.paper)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "%d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                    color = colors.ink,
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "对着麦克风说话",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleAction(
                    label = "✕",
                    bg = colors.card,
                    fg = colors.sub,
                    onClick = { doCancel() },
                )
                CircleAction(
                    label = "✓",
                    bg = colors.terra,
                    fg = colors.paper,
                    onClick = { doSend() },
                )
            }
        }
    }
}

@Composable
private fun MicPulse(amplitude: Int, color: Color, paperColor: Color) {
    val infinite = rememberInfiniteTransition(label = "mic-breath")
    val breath by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    // 实时音量峰值映射成外圈大小（0..32767 → 1..1.4）
    val voiceScale = (amplitude / 12000f).coerceIn(0f, 1.4f)
    val outerR = (96f + voiceScale * 40f + (breath - 1f) * 100f)
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = 0.12f),
                radius = outerR.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f),
            )
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 78.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            BigMicGlyph(paperColor)
        }
    }
}

@Composable
private fun BigMicGlyph(color: Color) {
    Canvas(modifier = Modifier.size(58.dp)) {
        val w = size.width
        val h = size.height
        val sw = 4.dp.toPx()
        // 麦克风胶囊
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.32f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.46f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f),
        )
        // 弧形支架
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.32f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.40f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw),
        )
        // 杆
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.66f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = sw,
        )
        // 底座
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.86f),
            end = Offset(w * 0.68f, h * 0.86f),
            strokeWidth = sw,
        )
    }
}

@Composable
private fun CircleAction(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = fg, style = MaterialTheme.typography.titleLarge)
    }
}
