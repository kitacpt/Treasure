package com.treasure

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
        setContent {
            TreasureTheme {
                TreasureNavHost()
            }
        }
    }
}
