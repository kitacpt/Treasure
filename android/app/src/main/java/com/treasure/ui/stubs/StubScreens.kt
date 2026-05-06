package com.treasure.ui.stubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors

@Composable
fun AddStubScreen() = StubBody(
    title = "录入",
    note = "对话式录入 · 拍照 → AI 自动识别 — coming",
)

@Composable
fun SettingsStubScreen() = StubBody(
    title = "Settings",
    note = "AI 服务 · BYO API key — coming",
)

@Composable
fun DetailStubScreen(itemId: String, onBack: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(colors.paper)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Detail",
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = itemId,
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "下一刀实现 · 点击任意处返回",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun StubBody(title: String, note: String) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp).padding(bottom = 80.dp),
        ) {
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = note,
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
