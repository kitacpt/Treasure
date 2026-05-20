package com.treasure.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.data.AiProfile
import com.treasure.data.AiProviderPreset
import com.treasure.data.modelSupportsVision
import com.treasure.theme.LocalTreasureColors
import com.treasure.theme.TreasureColors

@Composable
fun SettingsRoute(vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onActiveIndexChange = vm::setActiveIndex,
        onOpenAddSheet = vm::openAddSheet,
        onCloseAddSheet = vm::closeAddSheet,
        onAddProfile = vm::addProfile,
        onRemoveCurrentEditing = vm::removeCurrentEditingProfile,
        onSetAsDefault = vm::setAsDefault,
        onOpenEditor = vm::openEditor,
        onCloseEditor = vm::closeEditor,
        onSetPreset = vm::setPreset,
        onSetDisplayName = vm::setDisplayName,
        onSetBaseUrl = vm::setBaseUrl,
        onSetModel = vm::setModel,
        onSetApiKey = vm::setApiKey,
        onSetTemperature = vm::setTemperatureText,
        onSetThinking = vm::setThinkingEnabled,
        onSave = vm::save,
        onTest = vm::testConnection,
        onResetAll = vm::resetAll,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onActiveIndexChange: (Int) -> Unit,
    onOpenAddSheet: () -> Unit,
    onCloseAddSheet: () -> Unit,
    onAddProfile: (AiProviderPreset) -> Unit,
    onRemoveCurrentEditing: () -> Unit,
    onSetAsDefault: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onSetPreset: (AiProviderPreset) -> Unit,
    onSetDisplayName: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onResetAll: () -> Unit,
) {
    val colors = LocalTreasureColors.current

    androidx.activity.compose.BackHandler(enabled = state.editorTarget != null || state.addSheetOpen) {
        when {
            state.editorTarget != null -> onCloseEditor()
            state.addSheetOpen -> onCloseAddSheet()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 28.dp, bottom = 110.dp),
        ) {
            Header(
                profileCount = state.profiles.size,
            )
            // Cycle 0039：AI SERVICE 小标题跟模型卡片视觉成组 — 拉近到 10dp。
            //（Settings 大标题在 Header 内已被推到 20dp 之外，独立感保留。）
            Spacer(Modifier.height(10.dp))
            ProfilePager(
                state = state,
                onActiveIndexChange = onActiveIndexChange,
                onOpenAddSheet = onOpenAddSheet,
                onOpenEditor = onOpenEditor,
                onSetAsDefault = onSetAsDefault,
            )
            Spacer(Modifier.height(28.dp))
            BackupEntry(modifier = Modifier.padding(horizontal = 22.dp))
            Spacer(Modifier.height(28.dp))
            DangerZone(onReset = onResetAll, modifier = Modifier.padding(horizontal = 22.dp))
        }

        EditorDrawer(
            visible = state.editorTarget != null,
            state = state,
            onClose = onCloseEditor,
            onSetPreset = onSetPreset,
            onSetDisplayName = onSetDisplayName,
            onSetBaseUrl = onSetBaseUrl,
            onSetModel = onSetModel,
            onSetApiKey = onSetApiKey,
            onSetTemperature = onSetTemperature,
            onSetThinking = onSetThinking,
            onSave = onSave,
            onTest = onTest,
            onRemove = onRemoveCurrentEditing,
            canRemove = state.profiles.size > 1,
        )

        AddProfileSheet(
            visible = state.addSheetOpen,
            existing = state.profiles,
            onClose = onCloseAddSheet,
            onPick = onAddProfile,
        )
    }
}

// ─── header ──────────────────────────────────────────────────────────

@Composable
private fun Header(profileCount: Int) {
    val colors = LocalTreasureColors.current
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as com.treasure.TreasureApp
    val override by app.darkModeOverride.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDark = override ?: systemDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 22.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Settings",
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            // Cycle 0039：Settings 大字 → AI SERVICE 8 → 20dp 拉开 — 让 titleLarge
            // 与 labelSmall 不再视觉挤一坨，Settings 自己作为页面主标题独立感更强。
            Spacer(Modifier.height(20.dp))
            Text(
                text = "AI SERVICE · ${String.format("%02d", profileCount)}",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { app.setDarkModeOverride(!effectiveDark) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (effectiveDark) "☀" else "☾",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// ─── pager + cards ───────────────────────────────────────────────────

@Composable
private fun ProfilePager(
    state: SettingsUiState,
    onActiveIndexChange: (Int) -> Unit,
    onOpenAddSheet: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onSetAsDefault: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val pageCount = state.profiles.size + 1 // +1 ghost "add" card
    val pagerState = rememberPagerState(
        initialPage = state.activeIndex.coerceIn(0, maxOf(0, pageCount - 1)),
        pageCount = { pageCount },
    )

    // settle activeIndex back to VM as the user swipes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onActiveIndexChange(it) }
    }
    // when pager count grows (after add), VM bumps activeIndex; sync pager
    LaunchedEffect(state.activeIndex, pageCount) {
        if (state.activeIndex != pagerState.currentPage &&
            state.activeIndex in 0 until pageCount) {
            pagerState.animateScrollToPage(state.activeIndex)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill,
            pageSpacing = 0.dp,
            contentPadding = PaddingValues(horizontal = 22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp),
        ) { page ->
            if (page < state.profiles.size) {
                val p = state.profiles[page]
                ProfileCard(
                    profile = p,
                    isDefault = p.id == state.defaultProfileId,
                    onSetAsDefault = { onSetAsDefault(p.id) },
                    onEdit = { onOpenEditor(p.id) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                // Cycle 0035 v2：幽灵卡现在内容自适应，居中浮在 320dp 的 page
                // 高度里，看起来不那么"实心一大坨"。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AddGhostCard(onAdd = onOpenAddSheet)
                }
            }
        }

        // Cycle 0039：dots indicator 紧贴模型卡片底部 — 18 → 8dp，让 pager
        // 和 indicator 视觉上更像同一个组件。
        Spacer(Modifier.height(8.dp))

        // dots indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (i in 0 until pageCount) {
                val isGhost = i == pageCount - 1
                val isActive = i == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (isActive) 22.dp else 6.dp,
                    label = "dot-w",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            when {
                                isActive -> colors.ink
                                isGhost -> Color.Transparent
                                else -> colors.line
                            }
                        )
                        .border(
                            width = if (isGhost && !isActive) 1.dp else 0.dp,
                            color = if (isGhost && !isActive) colors.line else Color.Transparent,
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: AiProfile,
    isDefault: Boolean,
    onSetAsDefault: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profile.title,
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ConnectivityPill(profile)
        }

        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(colors.line))
        Spacer(Modifier.height(14.dp))

        FieldRow(label = "Model", value = profile.effectiveModel)
        if (profile.effectiveModel.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(84.dp))
                VisionChip(supportsVision = modelSupportsVision(profile.effectiveModel))
            }
        }
        Spacer(Modifier.height(10.dp))
        FieldRow(label = "Base URL", value = profile.effectiveBaseUrl.ifBlank { "—" }, mono = true)
        Spacer(Modifier.height(10.dp))
        FieldRow(
            label = "API Key",
            value = if (profile.hasKey) maskKey(profile.apiKey) else "未设置",
            mono = profile.hasKey,
        )

        Spacer(Modifier.height(20.dp))

        // Cycle 0035 v2：footer = [设为默认 / ★ 默认] (left) + [调整 →] (right)。
        // 移除按钮搬到编辑抽屉里。
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isDefault) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.terra.copy(alpha = 0.12f))
                        .border(0.5.dp, colors.terra.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "★ 默认",
                        color = colors.terra,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                        .clickable(onClick = onSetAsDefault)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "设为默认",
                        color = colors.ink,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "调整 →",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }
    }
}

/**
 * Cycle 0035 v2：幽灵卡缩小 — 之前跟正卡一样高 320dp，看起来胖、有点像
 * 空的占位。改成内容自适应高度（差不多 200dp），细虚线 + 居中 + 字体收紧。
 */
@Composable
private fun AddGhostCard(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = colors.line,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onAdd)
            .padding(vertical = 36.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "＋", color = colors.ink, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "添加 AI 服务",
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "NEW PROVIDER",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun FieldRow(label: String, value: String, mono: Boolean = false) {
    val colors = LocalTreasureColors.current
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(72.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = colors.ink,
            style = if (mono) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConnectivityPill(profile: AiProfile) {
    val colors = LocalTreasureColors.current
    val (label, dotColor) = when (profile.connectivity) {
        AiProfile.Connectivity.Unconfigured -> "未配置" to Color(0xFFC5392E)
        AiProfile.Connectivity.Idle -> "未连通" to Color(0xFFD89B23)
        AiProfile.Connectivity.Connected -> "已连通" to Color(0xFF3E8E45)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun VisionChip(supportsVision: Boolean) {
    val colors = LocalTreasureColors.current
    val (label, fg, bg, border) = if (supportsVision) {
        VisionChipStyle(
            label = "🖼 多模态",
            fg = colors.terra,
            bg = colors.terra.copy(alpha = 0.10f),
            border = colors.terra.copy(alpha = 0.55f),
        )
    } else {
        VisionChipStyle(
            label = "纯文本",
            fg = colors.sub,
            bg = Color.Transparent,
            border = colors.line,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = label, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

private data class VisionChipStyle(
    val label: String,
    val fg: Color,
    val bg: Color,
    val border: Color,
)

private fun maskKey(key: String): String {
    if (key.isBlank()) return "—"
    val tail = key.takeLast(4)
    return "•••• $tail"
}

// ─── add-profile bottom sheet ─────────────────────────────────────────

@Composable
private fun AddProfileSheet(
    visible: Boolean,
    existing: List<AiProfile>,
    onClose: () -> Unit,
    onPick: (AiProviderPreset) -> Unit,
) {
    val colors = LocalTreasureColors.current
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink.copy(alpha = 0.32f))
                .clickable(onClick = onClose),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(colors.paper)
                    .border(0.5.dp, colors.line)
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    // Cycle 0035 v2：底部给底导航胶囊腾出 ~96dp（胶囊高 ~50dp +
                    // 18dp 自身底偏移 + 安全余量），不然 sheet 内最后一行
                    // provider 被胶囊压住。
                    .padding(top = 14.dp, bottom = 96.dp)
                    .clickable(enabled = false) {},
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.line),
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "新增 AI 服务",
                        color = colors.ink,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "CHOOSE A PROVIDER",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(14.dp))
                AiProviderPreset.entries.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (preset == AiProviderPreset.Custom) Color.Transparent
                                else colors.card
                            )
                            .border(
                                width = if (preset == AiProviderPreset.Custom) 1.5.dp else 0.5.dp,
                                color = colors.line,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onPick(preset) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.display,
                                    color = colors.ink,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (preset.baseUrl != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = preset.defaultModel,
                                        color = colors.sub,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            Text(
                                text = if (preset == AiProviderPreset.Custom) "+" else "→",
                                color = colors.sub,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── backup / restore entry ───────────────────────────────────────────

/**
 * Cycle 0037：数据备份 / 恢复入口。
 *
 * 点击 → 弹 [com.treasure.backup.BackupSheet]，所有功能（导出 / 导入 /
 * 进度 / 二次确认）在里面完成。这一行只做"进门"。
 */
@Composable
private fun BackupEntry(modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("DATA", color = colors.sub, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line, RoundedCornerShape(14.dp))
                .clickable { open = true }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cycle 0039：左侧加线描档案箱 glyph（盖子 + 箱身 + 中间标签）
            // 取代原先无图标的入口；跟项目其他 glyph 同 1.5dp stroke 风格。
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.ink.copy(alpha = 0.06f))
                    .border(0.5.dp, colors.ink.copy(alpha = 0.32f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                com.treasure.backup.ArchiveGlyph(color = colors.ink)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "数据备份 / 恢复",
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "EXPORT · RESTORE",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text("→", color = colors.ink, style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (open) {
        com.treasure.backup.BackupSheet(onClose = { open = false })
    }
}

// ─── danger zone ──────────────────────────────────────────────────────

@Composable
private fun DangerZone(onReset: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    var confirming by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("DANGER ZONE", color = colors.sub, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line, RoundedCornerShape(14.dp))
                .clickable { confirming = true }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "重置设置",
                color = colors.terra,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text("→", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (confirming) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("重置设置？") },
            text = {
                Text(
                    text = "所有 AI 配置都会被清空，恢复成首装状态。这一步不可撤销。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onReset()
                    confirming = false
                }) { Text("重置", color = colors.terra) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirming = false }) {
                    Text("取消")
                }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

// ─── editor drawer (per-profile) ──────────────────────────────────────

@Composable
private fun EditorDrawer(
    visible: Boolean,
    state: SettingsUiState,
    onClose: () -> Unit,
    onSetPreset: (AiProviderPreset) -> Unit,
    onSetDisplayName: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    val colors = LocalTreasureColors.current

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink.copy(alpha = 0.32f))
                .clickable(onClick = onClose),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            EditorSheet(
                state = state,
                colors = colors,
                onClose = onClose,
                onSetPreset = onSetPreset,
                onSetDisplayName = onSetDisplayName,
                onSetBaseUrl = onSetBaseUrl,
                onSetModel = onSetModel,
                onSetApiKey = onSetApiKey,
                onSetTemperature = onSetTemperature,
                onSetThinking = onSetThinking,
                onSave = onSave,
                onTest = onTest,
                onRemove = onRemove,
                canRemove = canRemove,
            )
        }
    }
}

@Composable
private fun EditorSheet(
    state: SettingsUiState,
    colors: TreasureColors,
    onClose: () -> Unit,
    onSetPreset: (AiProviderPreset) -> Unit,
    onSetDisplayName: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    val draft = state.draft
    var revealKey by remember { mutableStateOf(false) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val canSave = draft.apiKey.isNotBlank() && draft.model.isNotBlank() &&
        (!draft.preset.baseUrlMandatory || draft.baseUrl.isNotBlank())
    val canTest = draft.apiKey.isNotBlank() && state.testStatus !is TestStatus.Running &&
        (!draft.preset.baseUrlMandatory || draft.baseUrl.isNotBlank())
    val showBaseUrlField = draft.preset.provider != com.treasure.core.ai.Provider.Anthropic
    val scrollState = rememberScrollState()

    LaunchedEffect(state.testStatus) {
        when (state.testStatus) {
            TestStatus.Ok, is TestStatus.Failed ->
                scrollState.animateScrollTo(scrollState.maxValue)
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.paper)
            .border(0.5.dp, colors.line)
            .clickable(enabled = false) {}
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp, bottom = 96.dp)
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.line),
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AI 配置",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "取消",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        // Cycle 0035 v2：显示名 — 默认为空，回退到 provider name；可改成
        // "工作 / 实验 / 本地 Ollama" 之类便于在卡片 + chatbar chip 上识别。
        FieldLabel("名称 · 默认用 ${draft.preset.display}")
        FormField(
            placeholder = draft.preset.display,
            value = draft.displayName,
            onValueChange = onSetDisplayName,
        )

        Spacer(Modifier.height(14.dp))
        FieldLabel("Provider")
        ProviderDropdown(
            current = draft.preset,
            open = dropdownOpen,
            onToggle = { dropdownOpen = !dropdownOpen },
            onPick = {
                onSetPreset(it)
                dropdownOpen = false
            },
        )

        if (showBaseUrlField) {
            Spacer(Modifier.height(14.dp))
            FieldLabel(if (draft.preset.baseUrlMandatory) "Base URL · 必填" else "Base URL")
            FormField(
                placeholder = draft.preset.baseUrl ?: "https://...",
                value = draft.baseUrl,
                onValueChange = onSetBaseUrl,
            )
        }

        Spacer(Modifier.height(14.dp))
        FieldLabel("Model")
        FormField(
            placeholder = draft.preset.defaultModel,
            value = draft.model,
            onValueChange = onSetModel,
        )
        Spacer(Modifier.height(8.dp))
        VisionChip(supportsVision = modelSupportsVision(draft.model.ifBlank { draft.preset.defaultModel }))

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("API Key")
            Spacer(Modifier.weight(1f))
            Text(
                text = if (revealKey) "隐藏" else "显示",
                color = colors.terra,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable { revealKey = !revealKey }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        FormField(
            placeholder = draft.preset.keyHint,
            value = draft.apiKey,
            onValueChange = onSetApiKey,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (revealKey) VisualTransformation.None
            else PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("高阶")

        Spacer(Modifier.height(10.dp))
        FieldLabel("Temperature · 0.0–2.0，留空走默认")
        FormField(
            placeholder = "如 0.20 / 0.70",
            value = draft.temperatureText,
            onValueChange = onSetTemperature,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Spacer(Modifier.height(14.dp))
        ToggleRow(
            label = "Enable thinking",
            checked = draft.thinkingEnabled,
            onCheckedChange = onSetThinking,
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                label = "保存",
                enabled = canSave,
                onClick = onSave,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                label = "测试连接",
                enabled = canTest,
                onClick = onTest,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        TestStatusLine(state.testStatus)

        if (canRemove) {
            // Cycle 0035 v2：移除按钮从卡片搬进编辑抽屉底。卡片只有一份时不
            // 让删（避免空状态），canRemove = profiles.size > 1。
            Spacer(Modifier.height(24.dp))
            SectionLabel("危险操作")
            Spacer(Modifier.height(10.dp))
            var confirmRemove by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.terra.copy(alpha = 0.55f))
                    .clickable { confirmRemove = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "移除此服务",
                    color = colors.terra,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (confirmRemove) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmRemove = false },
                    title = { Text("移除此服务？") },
                    text = {
                        Text(
                            text = "这份 AI 配置会被删除。如果它是默认服务，将自动切换到列表里的第一份。",
                            color = colors.sub,
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            confirmRemove = false
                            onRemove()
                        }) { Text("移除", color = colors.terra) }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { confirmRemove = false }) {
                            Text("取消")
                        }
                    },
                    containerColor = colors.paper,
                    titleContentColor = colors.ink,
                    textContentColor = colors.sub,
                )
            }
        }
    }
}

@Composable
private fun ProviderDropdown(
    current: AiProviderPreset,
    open: Boolean,
    onToggle: () -> Unit,
    onPick: (AiProviderPreset) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current.display,
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (current.baseUrl != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = current.baseUrl,
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                text = if (open) "▴" else "▾",
                color = colors.sub,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (open) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line))
            AiProviderPreset.entries.forEach { preset ->
                DropdownRow(
                    preset = preset,
                    selected = preset == current,
                    onClick = { onPick(preset) },
                )
            }
        }
    }
}

@Composable
private fun DropdownRow(
    preset: AiProviderPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) colors.paper else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.display,
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (preset.baseUrl != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = preset.baseUrl,
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "由你填写 base URL",
                    color = colors.sub,
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.terra),
            )
        }
    }
}

// ─── small reusable bits ──────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text,
            color = colors.ink,
            style = MaterialTheme.typography.labelSmall,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.line),
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        val handleX by animateDpAsState(
            targetValue = if (checked) 18.dp else 2.dp,
            label = "thinkingToggle",
        )
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) colors.terra.copy(alpha = 0.85f) else colors.line),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = handleX, top = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.paper),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    val colors = LocalTreasureColors.current
    Text(
        text = text,
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun FormField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = colors.sub.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(colors.terra),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            textStyle = LocalTextStyle.current.copy(
                color = colors.ink,
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (enabled) colors.ink else colors.card)
            .border(0.5.dp, if (enabled) colors.ink else colors.line)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) colors.paper else colors.sub,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, if (enabled) colors.terra.copy(alpha = 0.6f) else colors.line)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) colors.terra else colors.sub,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TestStatusLine(status: TestStatus) {
    val colors = LocalTreasureColors.current
    when (status) {
        TestStatus.Idle -> Text(
            text = "尚未测试",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        TestStatus.Running -> Text(
            text = "测试中…",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        TestStatus.Ok -> Text(
            text = "✓ 连接成功",
            color = colors.terra,
            style = MaterialTheme.typography.labelSmall,
        )
        is TestStatus.Failed -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(colors.terra.copy(alpha = 0.06f))
                .border(0.5.dp, colors.terra.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "× ${status.kind}",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = status.detail,
                color = colors.ink,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
