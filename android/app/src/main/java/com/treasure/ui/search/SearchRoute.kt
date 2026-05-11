package com.treasure.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.TreasureApp
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.BackArrow
import com.treasure.ui.components.HeroAvatar

/**
 * Cycle 0029：图鉴的搜索页。在 Grid 右上小搜索 icon 点开，全屏 push 上来，
 * 跟物品 Detail / Edit 同款返回。输入即时刷新结果；只对 brand / model /
 * nickname 三个字段做大小写不敏感包含匹配。命中段在标题上 terra 高亮。
 *
 * 布局：状态栏下一行 — [‹]  [搜索 ...]  ；下面 2 列 grid 同 GridScreen。
 */
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as TreasureApp
    val items by app.repository.items.collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val results = remember(q, items) {
        if (q.isBlank()) emptyList()
        else items.filter { it.matches(q) }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 头：[‹] 返回 + 搜索输入框；同 Detail / Edit 同款 BackArrow 风格
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackArrow(color = colors.ink, onClick = onBack)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.card)
                        .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索品牌 / 型号 / 昵称",
                            color = colors.sub.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.terra),
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.ink,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Search,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(
                        text = "✕",
                        color = colors.sub,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { query = "" }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // 结果数 / 引导
            val resultHint = when {
                q.isBlank() -> "输入关键词 — 立刻看结果"
                results.isEmpty() -> "没找到匹配项"
                else -> "${results.size} 条结果"
            }
            Text(
                text = resultHint,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 6.dp),
            )

            // 结果 grid — 跟 GridScreen 一致的 2 列卡片
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 22.dp, end = 22.dp, top = 8.dp, bottom = 80.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.id }) { item ->
                    SearchItemCard(
                        item = item,
                        query = q,
                        onClick = { onOpenItem(item.id) },
                    )
                }
                if (results.isEmpty() && q.isNotBlank()) {
                    item(span = { GridItemSpan(2) }) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun Item.matches(q: String): Boolean {
    val needle = q.lowercase()
    return brand.lowercase().contains(needle) ||
        model.lowercase().contains(needle) ||
        nickname.lowercase().contains(needle)
}

@Composable
private fun SearchItemCard(
    item: Item,
    query: String,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.1f)
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
            ) {
                HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(8.dp))
        // 标题：brand + model，命中段 terra 高亮
        Text(
            text = highlight("${item.brand} ${item.model}".trim(), query, colors.terra),
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        // 副标题：oneLiner 优先，没填回退 nickname；nickname 也高亮
        val sub = item.oneLiner.ifBlank { item.nickname }
        if (sub.isNotBlank()) {
            Text(
                text = highlight(sub, query, colors.terra),
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}

/**
 * 把 [haystack] 里所有 [needle] 出现的地方包成 terra 高亮的 SpanStyle。
 * 大小写不敏感匹配，但 annotated string 保留原文 case。空 needle 返回原文。
 */
private fun highlight(
    haystack: String,
    needle: String,
    accent: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    if (needle.isBlank()) return AnnotatedString(haystack)
    val lh = haystack.lowercase()
    val ln = needle.lowercase()
    return buildAnnotatedString {
        var i = 0
        while (i < haystack.length) {
            val hit = lh.indexOf(ln, i)
            if (hit < 0) {
                append(haystack.substring(i))
                break
            }
            append(haystack.substring(i, hit))
            withStyle(
                SpanStyle(
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(haystack.substring(hit, hit + needle.length))
            }
            i = hit + needle.length
        }
    }
}
