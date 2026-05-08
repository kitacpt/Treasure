@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.add

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.treasure.TreasureApp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.theme.LocalTreasureColors

private enum class AddMode(val label: String) {
    Manual("手动 录入"),
    Ai("AI 录入"),
}

private data class FormSession(
    val template: CategoryTemplate,
    val initial: ItemDraft? = null,
)

@Composable
fun AddRoute(
    onSaved: (String) -> Unit,
    onGoSettings: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var mode by remember { mutableStateOf(AddMode.Manual) }
    var session by remember { mutableStateOf<FormSession?>(null) }

    // AI availability is read on each composition so that toggling settings
    // and coming back updates the AI panel without restart.
    val context = LocalContext.current
    val aiAvailable = remember(context) {
        (context.applicationContext as? TreasureApp)?.settingsStore?.hasKey() == true
    }

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
                    AddMode.Manual -> Bubbles(onPick = { c ->
                        session = FormSession(CategoryTemplates.forCategory(c))
                    })
                    AddMode.Ai -> AiChatPanel(
                        aiAvailable = aiAvailable,
                        onGoSettings = onGoSettings,
                        onDraft = { draft ->
                            val cat = Category.fromId(draft.category ?: Category.TECH.id)
                            session = FormSession(
                                template = CategoryTemplates.forCategory(cat),
                                initial = draft,
                            )
                        },
                    )
                }
            }
        }
    }

    if (session != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { session = null },
            sheetState = sheetState,
            containerColor = colors.paper,
            contentColor = colors.ink,
        ) {
            CategoryForm(
                template = session!!.template,
                initial = session!!.initial,
                onCancel = { session = null },
                onSaved = { id ->
                    session = null
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

@Composable
private fun Bubbles(onPick: (Category) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerW = maxWidth
        val containerH = maxHeight
        val bubbleSize = listOf(containerW * 0.36f, containerH * 0.32f).min().coerceIn(96.dp, 168.dp)
        val cats = Category.entries
        val anchors = listOf(
            Alignment.TopStart    to Pair( 0.18f,  0.10f),
            Alignment.TopEnd      to Pair(-0.18f,  0.06f),
            Alignment.BottomStart to Pair( 0.10f, -0.18f),
            Alignment.BottomEnd   to Pair(-0.10f, -0.10f),
        )
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

private fun List<androidx.compose.ui.unit.Dp>.min(): androidx.compose.ui.unit.Dp =
    this.fold(this.first()) { acc, d -> if (d.value < acc.value) d else acc }
