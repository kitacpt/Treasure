package com.treasure.audio

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Cycle 0034 v2：简单 MediaPlayer wrapper — 给聊天气泡点击播放用。
 * 同一时间只能有一条在响（点新的会停掉旧的）。退出页面 DisposableEffect
 * release()。
 *
 * 暴露 [playing] 状态给 UI 渲染播放 / 暂停 icon。
 */
class VoicePlayerState {
    private var player: MediaPlayer? = null
    var playingPath: String? by mutableStateOf(null)
        private set

    fun toggle(path: String) {
        if (playingPath == path) {
            stop()
            return
        }
        stop()
        val f = File(path)
        if (!f.exists()) return
        val p = MediaPlayer()
        try {
            p.setDataSource(f.absolutePath)
            p.setOnCompletionListener { stop() }
            p.prepare()
            p.start()
            player = p
            playingPath = path
        } catch (e: Exception) {
            runCatching { p.release() }
            player = null
            playingPath = null
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playingPath = null
    }
}

val LocalVoicePlayer = compositionLocalOf<VoicePlayerState> { VoicePlayerState() }

@Composable
fun rememberVoicePlayer(): VoicePlayerState {
    val state = remember { VoicePlayerState() }
    DisposableEffect(state) { onDispose { state.stop() } }
    return state
}
