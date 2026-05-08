package com.treasure.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.ai.Provider
import com.treasure.theme.LocalTreasureColors

@Composable
fun SettingsRoute(vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onProviderChange = vm::setProvider,
        onBaseUrlChange = vm::setBaseUrl,
        onModelChange = vm::setModel,
        onApiKeyChange = vm::setApiKey,
        onSave = vm::save,
        onClear = vm::clear,
        onTest = vm::testConnection,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onProviderChange: (Provider) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onTest: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var revealKey by remember { mutableStateOf(false) }
    val showBaseUrl = state.provider != Provider.Anthropic // baseUrl optional otherwise

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 28.dp, bottom = 100.dp),
        ) {
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

            Spacer(Modifier.height(20.dp))
            Text(
                text = "用户自带 API key（设备直连，不走代理）",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("Provider")
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Provider.entries.forEach { p ->
                    ProviderChip(
                        label = p.display,
                        selected = p == state.provider,
                        onClick = { onProviderChange(p) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            FieldLabel("Model")
            FormField(
                placeholder = SettingsViewModel::class.let { "默认见 provider" },
                value = state.model,
                onValueChange = onModelChange,
            )

            if (showBaseUrl) {
                Spacer(Modifier.height(14.dp))
                FieldLabel(if (state.provider == Provider.OpenAiCompatible) "Base URL · 必填" else "Base URL · 可选")
                FormField(
                    placeholder = "https://api.openai.com",
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldLabel("API Key")
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (revealKey) "隐藏" else "显示",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { revealKey = !revealKey },
                )
            }
            FormField(
                placeholder = when (state.provider) {
                    Provider.Anthropic -> "sk-ant-..."
                    Provider.OpenAi -> "sk-..."
                    Provider.OpenAiCompatible -> "your-key"
                },
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (revealKey) VisualTransformation.None
                else PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "存在 EncryptedSharedPreferences，仅本机可读",
                color = colors.sub.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(Modifier.height(20.dp))
            val canSave = state.apiKey.isNotBlank() && state.model.isNotBlank() &&
                (state.provider != Provider.OpenAiCompatible || state.baseUrl.isNotBlank())
            val canTest = state.apiKey.isNotBlank() && state.testStatus !is TestStatus.Running &&
                (state.provider != Provider.OpenAiCompatible || state.baseUrl.isNotBlank())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    label = "保存",
                    enabled = canSave,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    label = "测试",
                    enabled = canTest,
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            TestStatusLine(state.testStatus, configured = state.keyConfigured)

            Spacer(Modifier.height(36.dp))
            Text("DANGER ZONE", color = colors.sub, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.line)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "清除所有设置",
                    color = colors.terra,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text("→", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "API key 会从本机抹除",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.ink else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                0.5.dp,
                if (selected) colors.ink else colors.line,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colors.paper else colors.ink,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TestStatusLine(status: TestStatus, configured: Boolean) {
    val colors = LocalTreasureColors.current
    val (text, color) = when (status) {
        TestStatus.Idle ->
            (if (configured) "已配置 · 可在录入页用 AI" else "尚未配置") to colors.sub
        TestStatus.Running -> "测试中..." to colors.sub
        TestStatus.Ok -> "✓ 连接成功" to colors.terra
        is TestStatus.Failed -> "× ${status.message}" to colors.terra
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
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
