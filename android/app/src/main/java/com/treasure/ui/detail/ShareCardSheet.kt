@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.treasure.TreasureApp
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cycle 0038 — 分享卡片底部抽屉。点击 DetailScreen 右上角 share icon →
 *
 * 1) 立刻弹出抽屉，显示 spinner + "正在生成卡片…"
 * 2) IO 线程查 [CategoryRepository] 拿到分类显示名 + 调 [ShareCard.generate]
 *    画图 → 落到 filesDir/share-cards/
 * 3) 完成后切换到预览图 + [保存到相册] / [分享到其他 app] 两个按钮
 *
 * 关闭抽屉时不主动删生成的文件（同物品再次分享可复用，磁盘累积留下一刀）。
 */
@Composable
fun ShareCardSheet(
    item: Item,
    onDismiss: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var cardFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // 进入即在 IO 线程拉分类显示名 + 生成卡片
    LaunchedEffect(item.id) {
        error = null
        cardFile = null
        val app = context.applicationContext as TreasureApp
        runCatching {
            withContext(Dispatchers.IO) {
                val infos = app.categoryRepository.loadAll()
                val info = infos.firstOrNull { it.id == item.category }
                val zh = info?.nameZh ?: Category.fromId(item.category).nameZh
                val en = info?.nameEn ?: Category.fromId(item.category).nameEn
                ShareCard.generate(context, item, zh, en)
            }
        }.onSuccess { cardFile = it }
            .onFailure { error = it.message ?: "卡片生成失败" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            // 顶部标题条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "分享卡片",
                    color = colors.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "1080 · 1350",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.line),
            )
            Spacer(Modifier.height(20.dp))

            // 预览区：4:5 的 box 占满宽度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card)
                    .border(0.5.dp, colors.line),
                contentAlignment = Alignment.Center,
            ) {
                val f = cardFile
                if (f != null) {
                    // 预览框按 4:5 等比铺满宽度 — 直接 aspectRatio
                    AsyncImage(
                        model = f,
                        contentDescription = "卡片预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 5f),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 5f)
                            .padding(40.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            color = colors.terra,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = error ?: "正在生成卡片…",
                            color = colors.sub,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 两个按钮：左 [保存到相册]（轮廓）/ 右 [分享到其他 app]（terra 实心）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                        .clickable(enabled = cardFile != null) {
                            scope.launch {
                                val f = cardFile ?: return@launch
                                val uri = withContext(Dispatchers.IO) {
                                    ShareCard.saveToGallery(context, f)
                                }
                                val msg = if (uri != null) "已保存到相册 · Pictures/Treasure"
                                else "保存失败 · 检查存储权限"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "保存到相册",
                        color = if (cardFile != null) colors.ink else colors.sub.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (cardFile != null) colors.terra else colors.terra.copy(alpha = 0.30f),
                        )
                        .clickable(enabled = cardFile != null) {
                            val f = cardFile ?: return@clickable
                            runCatching {
                                context.startActivity(ShareCard.shareIntent(context, f))
                            }.onFailure {
                                Toast.makeText(context, "分享失败 · ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "分享到其他 app",
                        color = colors.paper,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "卡片永久 1080 × 1350，4:5 比例。微信 / 朋友圈 / 邮件均可。",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
