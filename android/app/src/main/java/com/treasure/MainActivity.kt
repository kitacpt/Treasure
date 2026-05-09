package com.treasure

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.treasure.theme.TreasureTheme
import com.treasure.ui.nav.TreasureNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status / nav bars; each screen handles its own
        // insets padding so the canvas extends edge-to-edge consistently.
        enableEdgeToEdge()
        // Cycle 0019：从京东 / 淘宝 等用 ACTION_SEND 分享过来的文字（含商品
        // 链接）落到 TreasureApp.shareIntake，再由 MainScreen 切到录入页 +
        // 自动派给 AI。
        consumeShareIntent(intent)
        setContent {
            TreasureTheme {
                TreasureNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeShareIntent(intent)
    }

    private fun consumeShareIntent(intent: Intent?) {
        intent ?: return
        val text = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }?.trim().orEmpty()
        if (text.isNotEmpty()) {
            (application as TreasureApp).shareIntake.value = text
        }
    }
}
