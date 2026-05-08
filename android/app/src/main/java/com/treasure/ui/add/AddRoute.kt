@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.treasure.core.domain.Category
import com.treasure.theme.LocalTreasureColors

private enum class AddMode(val label: String) {
    Manual("手动 录入"),
    Ai("AI 录入"),
}

@Composable
fun AddRoute(
    onSaved: (String) -> Unit,
    onGoSettings: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var mode by remember { mutableStateOf(AddMode.Manual) }
    var picked by remember { mutableStateOf<Category?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 100.dp)) {
            Header()
            Spacer(Modifier.height(14.dp))
            ModeToggle(mode = mode, onModeChange = { mode = it })
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (mode) {
                    AddMode.Manual -> Bubbles(onPick = { picked = it })
                    AddMode.Ai     -> AiChatStub(onGoSettings = onGoSettings)
                }
            }
        }
    }

    if (picked != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { picked = null },
            sheetState = sheetState,
            containerColor = colors.paper,
            contentColor = colors.ink,
        ) {
            CategoryForm(
                template = CategoryTemplates.forCategory(picked!!),
                onCancel = { picked = null },
                onSaved = { id ->
                    picked = null
                    onSaved(id)
                },
            )
        }
    }
}

@Composable
private fun Header() {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Text(
            text = "Treasure",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "NEW ENTRY",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ModeToggle(mode: AddMode, onModeChange: (AddMode) -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AddMode.entries.forEach { m ->
            val on = m == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) colors.ink else androidx.compose.ui.graphics.Color.Transparent)
                    .border(
                        0.5.dp,
                        if (on) colors.ink else colors.line,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onModeChange(m) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = m.label,
                    color = if (on) colors.paper else colors.ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ─── Manual: bubbles ────────────────────────────────────────────────────────

@Composable
private fun Bubbles(onPick: (Category) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerW = maxWidth
        val containerH = maxHeight
        // Bubble diameter scales with the smaller axis; clamp to a reasonable range
        val bubbleSize = listOf(containerW * 0.36f, containerH * 0.32f).min().coerceIn(96.dp, 168.dp)
        val cats = Category.entries
        // 2×2 layout: anchor each bubble to a corner via fractional positioning
        val anchors = listOf(
            Alignment.TopStart    to Pair( 0.18f, 0.10f),  // top-left, slight inward shift
            Alignment.TopEnd      to Pair(-0.18f, 0.06f),
            Alignment.BottomStart to Pair( 0.10f,-0.18f),
            Alignment.BottomEnd   to Pair(-0.10f,-0.10f),
        )
        // Different float periods so the motion never feels metronomic
        val periods = listOf(2200, 2700, 2400, 2900)
        cats.forEachIndexed { idx, cat ->
            val (alignment, shift) = anchors[idx]
            FloatingBubble(
                category = cat,
                size = bubbleSize,
                periodMs = periods[idx],
                modifier = Modifier
                    .align(alignment)
                    .padding(
                        start = (containerW * shift.first.coerceAtLeast(0f)),
                        end = (containerW * (-shift.first).coerceAtLeast(0f)),
                        top = (containerH * shift.second.coerceAtLeast(0f)),
                        bottom = (containerH * (-shift.second).coerceAtLeast(0f)),
                    ),
                onClick = { onPick(cat) },
            )
        }
    }
}

@Composable
private fun FloatingBubble(
    category: Category,
    size: androidx.compose.ui.unit.Dp,
    periodMs: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val transition = rememberInfiniteTransition(label = "bubble-${category.id}")
    val offsetY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "y",
    )
    Column(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = offsetY }
            .clip(CircleShape)
            .background(colors.card)
            .border(0.5.dp, colors.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(size * 0.16f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryGlyph(
            category = category,
            color = colors.ink,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f),
        )
        Spacer(Modifier.height(size * 0.04f))
        Text(
            text = category.nameZh,
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = category.nameEn.uppercase(),
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ─── AI mode: chat scaffold stub ───────────────────────────────────────────

@Composable
private fun AiChatStub(onGoSettings: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
    ) {
        // Pretend bubble
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(14.dp),
        ) {
            Text(
                text = "嗨，告诉我你新收下的物件是什么 — 拍个照、描述几句、或者贴一段商品页都行。",
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.End)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(colors.ink)
                .padding(14.dp),
        ) {
            Text(
                text = "...",
                color = colors.paper.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(40.dp))
        // "Coming" panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AI 录入 — coming",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "需要先在「设置」里配置 API key 才能聊起来",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(0.5.dp, colors.terra.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onGoSettings)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "去设置",
                    color = colors.terra.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// helper — Compose has no built-in Dp.min; we list-it
private fun List<androidx.compose.ui.unit.Dp>.min(): androidx.compose.ui.unit.Dp =
    this.fold(this.first()) { acc, d -> if (d.value < acc.value) d else acc }
