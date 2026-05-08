package com.treasure.ui.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.ai.ItemDraft
import com.treasure.theme.LocalTreasureColors

private sealed interface AiState {
    data object Idle : AiState
    data object Running : AiState
    data class Error(val message: String) : AiState
}

@Composable
fun AiChatPanel(
    aiAvailable: Boolean,
    onGoSettings: () -> Unit,
    onDraft: (ItemDraft) -> Unit,
    vm: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    var input by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var state by remember { mutableStateOf<AiState>(AiState.Idle) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
    ) {
        if (!aiAvailable) {
            NotConfiguredCard(onGoSettings = onGoSettings)
            Spacer(Modifier.height(20.dp))
        }

        // Assistant bubble (always visible — sets the tone)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(14.dp),
        ) {
            Text(
                text = "嗨。告诉我这件物品 — 描述几句，或者贴一张图。我会帮你填好基本信息，你再调整。",
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.weight(1f))

        // Image preview row (if attached)
        imageUri?.let { uri ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "图片已附",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "移除",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable { imageUri = null }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // Input box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (input.isEmpty()) {
                Text(
                    text = "比如：Yonex Astrox 99 Pro，桃田款 …",
                    color = colors.sub.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                cursorBrush = SolidColor(colors.terra),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 160.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Action row: image + send
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
                    .clickable(enabled = aiAvailable && state !is AiState.Running) {
                        pickPhoto.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (imageUri == null) "+ 选张图" else "换张图",
                    color = colors.terra,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.weight(1f))

            val canSend = aiAvailable &&
                state !is AiState.Running &&
                (input.isNotBlank() || imageUri != null)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (canSend) colors.ink else colors.card)
                    .border(0.5.dp, if (canSend) colors.ink else colors.line)
                    .clickable(enabled = canSend) {
                        state = AiState.Running
                        vm.extractDraft(
                            text = input,
                            imageUri = imageUri,
                            onDraft = { draft ->
                                state = AiState.Idle
                                onDraft(draft)
                                input = ""
                                imageUri = null
                            },
                            onError = { msg ->
                                state = AiState.Error(msg)
                            },
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = when (state) {
                        AiState.Running -> "处理中…"
                        else -> "发送"
                    },
                    color = if (canSend) colors.paper else colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        (state as? AiState.Error)?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "× ${err.message}",
                color = colors.terra,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NotConfiguredCard(onGoSettings: () -> Unit) {
    val colors = LocalTreasureColors.current
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
            text = "AI 录入需要先配置 API key",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "BYO key · 设备直连 Anthropic",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(0.5.dp, colors.terra.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                .clickable(onClick = onGoSettings)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = "去设置",
                color = colors.terra,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
