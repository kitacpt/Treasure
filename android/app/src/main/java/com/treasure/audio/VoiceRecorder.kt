package com.treasure.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Cycle 0034 v2：简单包装 MediaRecorder — 录制 AAC（容器 m4a，48kHz、单声
 * 道、64kbps）。够 LLM 听清说话内容，也够本地回放。
 *
 * 设计：每条录音一个 instance。start() 立刻起，stop() 返回最终文件
 * 路径与时长字符串（"m:ss"）。出错 / 用户取消 → cancel() 删文件。
 */
class VoiceRecorder(
    private val context: Context,
    /** 保到 filesDir/voice-cache/<convoId>/<uuid>.m4a；调用方传完整 dest。 */
    private val destFile: File,
) {
    private var recorder: MediaRecorder? = null
    private var startedAtMs: Long = 0L

    /** 录制中。 */
    val isRecording: Boolean get() = recorder != null

    fun start() {
        destFile.parentFile?.mkdirs()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(48_000)
            r.setAudioEncodingBitRate(64_000)
            r.setOutputFile(destFile.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            startedAtMs = System.currentTimeMillis()
        } catch (e: Exception) {
            runCatching { r.release() }
            recorder = null
            throw e
        }
    }

    /** 返回 (文件, 时长字符串 m:ss)；start 没成功 / 已 stop 返 null。 */
    fun stop(): Pair<File, String>? {
        val r = recorder ?: return null
        recorder = null
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        return try {
            runCatching { r.stop() }
            runCatching { r.release() }
            destFile to formatDuration(elapsedMs)
        } catch (e: Exception) {
            runCatching { r.release() }
            destFile.delete()
            null
        }
    }

    fun cancel() {
        val r = recorder ?: return
        recorder = null
        runCatching { r.stop() }
        runCatching { r.release() }
        destFile.delete()
    }

    /** 当前正在录的实时音量峰值（0..32767）。MediaRecorder 提供。 */
    fun currentAmplitude(): Int = runCatching {
        recorder?.maxAmplitude ?: 0
    }.getOrDefault(0)

    /** 当前已录时长（ms）。给 UI 计时用。 */
    fun elapsedMs(): Long = if (recorder == null) 0L else System.currentTimeMillis() - startedAtMs

    companion object {
        fun newFile(context: Context, conversationId: String): File {
            val dir = File(context.filesDir, "voice-cache/$conversationId")
            return File(dir, "${UUID.randomUUID()}.m4a")
        }

        fun formatDuration(ms: Long): String {
            val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }
    }
}
