package com.treasure.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.treasure.data.AiProviderPreset
import com.treasure.data.modelSupportsVision
import com.treasure.theme.LocalTreasureColors
import com.treasure.theme.TreasureColors

@Composable
fun SettingsRoute(vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpenEditor = vm::openEditor,
        onCloseEditor = vm::closeEditor,
        onSetPreset = vm::setPreset,
        onSetBaseUrl = vm::setBaseUrl,
        onSetModel = vm::setModel,
        onSetApiKey = vm::setApiKey,
        onSetTemperature = vm::setTemperatureText,
        onSetThinking = vm::setThinkingEnabled,
        onSave = vm::save,
        onTest = vm::testConnection,
        onClear = vm::clear,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
    onSetPreset: (AiProviderPreset) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalTreasureColors.current

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
                .padding(horizontal = 22.dp)
                .padding(top = 28.dp, bottom = 110.dp),
        ) {
            Header()
            Spacer(Modifier.height(28.dp))
            AiSummaryCard(
                saved = state.saved,
                onEdit = onOpenEditor,
            )
            Spacer(Modifier.height(36.dp))
            DangerZone(onClear = onClear)
        }

        EditorDrawer(
            visible = state.editorOpen,
            state = state,
            onClose = onCloseEditor,
            onSetPreset = onSetPreset,
            onSetBaseUrl = onSetBaseUrl,
            onSetModel = onSetModel,
            onSetApiKey = onSetApiKey,
            onSetTemperature = onSetTemperature,
            onSetThinking = onSetThinking,
            onSave = onSave,
            onTest = onTest,
        )
    }
}

// ─── summary ──────────────────────────────────────────────────────────

@Composable
private fun Header() {
    val colors = LocalTreasureColors.current
    Column {
        Text(
            text = "Settings",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "AI SERVICE",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AiSummaryCard(
    saved: SavedConfig,
    onEdit: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .clickable(onClick = onEdit)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = saved.preset.display,
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ConnectivityPill(saved = saved)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line),
        )
        Spacer(Modifier.height(14.dp))
        ModelRow(model = saved.model)
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "Base URL",
            value = saved.baseUrl.ifBlank { saved.preset.baseUrl ?: "—" },
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "API Key",
            value = if (saved.keyConfigured) maskKey(saved.apiKey) else "未配置",
            mono = saved.keyConfigured,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "调整 →",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Cycle 0022/0023：编辑抽屉里 model 输入下面的能力提示。两种状态都显示，
 * 用户改 model 名实时切换。文案精简到只有 "多模态" / "纯文本"，没有备注。
 */
@Composable
private fun ModelCapabilityHint(model: String) {
    val supportsVision = model.isNotBlank() && modelSupportsVision(model)
    Spacer(Modifier.height(8.dp))
    VisionChip(supportsVision = supportsVision)
}

@Composable
private fun ModelRow(model: String) {
    val colors = LocalTreasureColors.current
    val display = model.ifBlank { "—" }
    val showChip = model.isNotBlank()
    val supportsVision = showChip && modelSupportsVision(model)
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "Model",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(72.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display,
                color = colors.ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (showChip) {
                Spacer(Modifier.height(8.dp))
                VisionChip(supportsVision = supportsVision)
            }
        }
    }
}

/**
 * 双状态 pill：vision-capable 走 terra（与品牌色一致 + 显眼）；纯文本走
 * 浅灰 outline。两种文案都只有 2-3 个汉字，没有冗余备注。
 */
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
            bg = androidx.compose.ui.graphics.Color.Transparent,
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
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private data class VisionChipStyle(
    val label: String,
    val fg: androidx.compose.ui.graphics.Color,
    val bg: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
)

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
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
private fun ConnectivityPill(saved: SavedConfig) {
    val colors = LocalTreasureColors.current
    val (label, dotColor) = when {
        !saved.keyConfigured ->
            "未配置" to androidx.compose.ui.graphics.Color(0xFFC5392E) // 红
        !saved.lastTestPassed ->
            "未连通" to androidx.compose.ui.graphics.Color(0xFFD89B23) // 黄
        else ->
            "已连通" to androidx.compose.ui.graphics.Color(0xFF3E8E45) // 绿
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

private fun maskKey(key: String): String {
    if (key.isBlank()) return "—"
    val tail = key.takeLast(4)
    return "•••• $tail"
}

// ─── danger zone ──────────────────────────────────────────────────────

@Composable
private fun DangerZone(onClear: () -> Unit) {
    val colors = LocalTreasureColors.current
    var confirming by remember { mutableStateOf(false) }
    Text("DANGER ZONE", color = colors.sub, style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, colors.line)
            .clickable { confirming = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
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

    if (confirming) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("重置设置？") },
            text = {
                Text(
                    text = "API key、provider、temperature、thinking 等所有 AI 配置都会被清空，恢复成首装状态。这一步不可撤销。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onClear()
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

// ─── editor drawer ────────────────────────────────────────────────────

@Composable
private fun EditorDrawer(
    visible: Boolean,
    state: SettingsUiState,
    onClose: () -> Unit,
    onSetPreset: (AiProviderPreset) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
) {
    val colors = LocalTreasureColors.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Scrim — taps outside the sheet dismiss the drawer.
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
                onSetBaseUrl = onSetBaseUrl,
                onSetModel = onSetModel,
                onSetApiKey = onSetApiKey,
                onSetTemperature = onSetTemperature,
                onSetThinking = onSetThinking,
                onSave = onSave,
                onTest = onTest,
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
    onSetBaseUrl: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetThinking: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
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

    // 测试结果出来时把抽屉滚到底，让 ✓ / × 行立刻可见，省得用户自己滑。
    androidx.compose.runtime.LaunchedEffect(state.testStatus) {
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
            .clickable(enabled = false) {} // swallow scrim taps
            // imePadding 替代 navigationBarsPadding：键盘弹出时自动把整张抽屉
            // 上推；闲时等于 navigationBars 的高度。
            .imePadding()
            .navigationBarsPadding()
            // 给底部留出 control island 的高度（胶囊高 ~50dp + 18dp 自身底
            // padding + 14dp 安全缓冲），避免抽屉里的 [保存] 按钮被胶囊遮住
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
        // Cycle 0022：根据当前 model 名给个能力提示。多模态 → 可发图。
        ModelCapabilityHint(model = draft.model.ifBlank { draft.preset.defaultModel })

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
        // 自家小开关：terra 圆点滑动
        val handleX by androidx.compose.animation.core.animateDpAsState(
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
