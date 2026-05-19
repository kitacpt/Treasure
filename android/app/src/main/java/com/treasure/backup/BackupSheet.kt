@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.treasure.TreasureApp
import com.treasure.theme.LocalTreasureColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cycle 0037：数据备份 / 恢复抽屉。
 *
 * 单抽屉里串起 3 屏（state machine）：
 *   1. **Choose**：两块大卡片（导出 / 导入），底部一行 "DATA · BACKUP" 标签。
 *   2. **Confirm import**：AlertDialog 二次确认（仅导入路径用）。"会覆盖
 *      当前所有数据，建议先导出当前数据作为安全副本"。
 *   3. **Progress**：进度条 + 当前阶段文字。完成态变成 "✓ 已导出 N / [完成]"
 *      或 "✓ 已恢复 N / [完成]"；失败态显示红框 + [重试] + [关闭]。
 *
 * 整个抽屉不依赖外部 VM — 内部用 [rememberCoroutineScope] 起 IO 任务，把
 * [BackupService] 的回调桥到 Compose `MutableState<BackupProgress>`。这样
 * 这一块和 SettingsViewModel 解耦，将来移走 / 复用都不破其它代码。
 */
@Composable
fun BackupSheet(
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TreasureApp
    val service = remember {
        BackupService(app, app.repository, app.categoryRepository, app.conversationRepository)
    }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<BackupProgress>(BackupProgress.Idle) }
    var confirmImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val colors = LocalTreasureColors.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                progress = BackupProgress.Running("准备导出 ...", percent = 0)
                withContext(Dispatchers.IO) {
                    service.exportToZip(uri) { p -> progress = p }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) confirmImportUri = uri
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = {
            // 导出 / 导入运行中不要让 sheet 关 — 用户多半是手抖滑下去。
            if (progress is BackupProgress.Running) return@ModalBottomSheet
            onClose()
        },
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .heightIn(min = 280.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "数据备份 / 恢复",
                        color = colors.ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "DATA · BACKUP",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            when (val p = progress) {
                is BackupProgress.Idle -> ChooseScreen(
                    onExport = {
                        exportLauncher.launch(BackupService.suggestedFileName())
                    },
                    onImport = {
                        importLauncher.launch(arrayOf("application/zip"))
                    },
                )
                is BackupProgress.Running -> ProgressScreen(p)
                is BackupProgress.ExportDone -> ExportDoneScreen(p, onFinish = {
                    progress = BackupProgress.Idle
                    onClose()
                })
                is BackupProgress.ImportDone -> ImportDoneScreen(p, onFinish = {
                    progress = BackupProgress.Idle
                    onClose()
                })
                is BackupProgress.Failed -> FailedScreen(
                    failure = p,
                    onRetry = { progress = BackupProgress.Idle },
                    onClose = {
                        progress = BackupProgress.Idle
                        onClose()
                    },
                )
            }
        }
    }

    // 二次确认 AlertDialog（导入路径）
    val pendingUri = confirmImportUri
    if (pendingUri != null) {
        AlertDialog(
            onDismissRequest = { confirmImportUri = null },
            title = { Text("恢复备份？") },
            text = {
                Text(
                    text = "这会清空当前的物品、分类和会话历史，并用备份里的数据覆盖。" +
                        "建议先点 [导出] 把当前数据保存一份做安全副本，再来恢复。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingUri
                    confirmImportUri = null
                    scope.launch {
                        progress = BackupProgress.Running("准备恢复 ...", percent = 0)
                        withContext(Dispatchers.IO) {
                            service.importFromZip(uri) { p -> progress = p }
                        }
                    }
                }) { Text("继续恢复", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImportUri = null }) {
                    Text("取消")
                }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

// ─── screen 1: choose ────────────────────────────────────────────────────

@Composable
private fun ChooseScreen(
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    ActionRow(
        icon = "↗",
        title = "导出备份",
        subtitle = "EXPORT · 打包所有物品、分类、会话和实拍照片为 zip",
        accent = colors.ink,
        onClick = onExport,
    )
    Spacer(Modifier.height(10.dp))
    ActionRow(
        icon = "↙",
        title = "从备份恢复",
        subtitle = "RESTORE · 用之前的 zip 整体覆盖当前数据",
        accent = colors.terra,
        onClick = onImport,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = "AI 密钥不会打进备份。换机后请在「Settings → 调整」重新填写。",
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
        fontStyle = FontStyle.Italic,
    )
}

@Composable
private fun ActionRow(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f))
                .border(0.5.dp, accent.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, color = accent, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(text = "→", color = colors.sub, style = MaterialTheme.typography.bodyLarge)
    }
}

// ─── screen 2: progress ──────────────────────────────────────────────────

@Composable
private fun ProgressScreen(state: BackupProgress.Running) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.stage,
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(14.dp))
        BackupProgressBar(percent = state.percent)
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                state.percent < 0 -> "正在处理 ..."
                else -> "${state.percent}%"
            },
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        if (state.detail.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.detail,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * 简单的 paper / line 风进度条。
 * percent < 0 = indeterminate（一根贯穿的弱条作 "正在处理" 提示）。
 */
@Composable
private fun BackupProgressBar(percent: Int) {
    val colors = LocalTreasureColors.current
    val isIndeterminate = percent < 0
    val ratio = if (isIndeterminate) 1f else (percent.coerceIn(0, 100) / 100f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.line.copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = ratio)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isIndeterminate) colors.line else colors.terra,
                ),
        )
    }
}

// ─── screen 3a: export done ──────────────────────────────────────────────

@Composable
private fun ExportDoneScreen(state: BackupProgress.ExportDone, onFinish: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "✓ 已导出",
            color = colors.terra,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        SummaryLine("文件", state.fileName)
        SummaryLine("大小", formatSize(state.sizeBytes))
        SummaryLine("物品", "${state.items} 件")
        SummaryLine("照片", "${state.photoFiles} 张")
        Spacer(Modifier.height(18.dp))
        DoneButton(label = "完成", onClick = onFinish)
    }
}

@Composable
private fun ImportDoneScreen(state: BackupProgress.ImportDone, onFinish: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "✓ 已恢复",
            color = colors.terra,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        SummaryLine("物品", "${state.items} 件")
        SummaryLine("照片", "${state.photoFiles} 张")
        Spacer(Modifier.height(10.dp))
        Text(
            text = "可能需要回到门厅或图鉴页一次让缓存刷新。",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(18.dp))
        DoneButton(label = "完成", onClick = onFinish)
    }
}

@Composable
private fun FailedScreen(
    failure: BackupProgress.Failed,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "× ${failure.stage}失败",
            color = colors.terra,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.terra.copy(alpha = 0.06f))
                .border(0.5.dp, colors.terra.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = failure.message,
                color = colors.ink,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.line)
                    .clickable(onClick = onClose)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "关闭", color = colors.sub, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.ink)
                    .clickable(onClick = onRetry)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "重试", color = colors.paper, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ─── small bits ──────────────────────────────────────────────────────────

@Composable
private fun SummaryLine(label: String, value: String) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = colors.ink,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DoneButton(label: String, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.ink)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = colors.paper, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 0) return "—"
    val mb = bytes / 1024.0 / 1024.0
    if (mb >= 1.0) return "%.1f MB".format(mb)
    val kb = bytes / 1024.0
    if (kb >= 1.0) return "%.0f KB".format(kb)
    return "$bytes B"
}

/**
 * 用 LaunchedEffect 自动让 sheet 在恢复成功后多停一拍 — 暂时只有静态展示，
 * 用户必须点 [完成] 才关，方便他读完。
 * (保留 LaunchedEffect import 以便未来扩展，比如自动重启 app。)
 */
@Suppress("unused")
@Composable
private fun NoopAutoReset() {
    LaunchedEffect(Unit) {}
}
