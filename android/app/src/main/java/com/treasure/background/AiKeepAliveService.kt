package com.treasure.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.treasure.R

/**
 * Cycle 0031：AI 调用期间用的前台保活服务。
 *
 * vivo / 华为 / OPPO / 小米 系统在熄屏 / 切后台几秒后就会冻结普通进程 ——
 * OkHttp socket 立刻断，用户看到的就是 "Software caused connection abort" /
 * "Stream closed"。前台服务 (`startForeground` + 可见通知) 是 Android 唯一
 * 让 OEM 厂商也不敢碾的"我在干活儿"信号。
 *
 * 流程：
 *   - [start] 在 AI extract 开始时 startForegroundService → 弹小通知
 *     "正在和 AI 对话…" + 拿 PARTIAL_WAKE_LOCK 防 CPU 睡。
 *   - service 内部不主动做事，仅顶住进程；真正的 HTTP 调用在 AddViewModel
 *     的 viewModelScope 里跑。
 *   - 完成 / 失败 → [stop] stopService + 释放 wake lock。
 */
class AiKeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Treasure:ai-extract",
            ).apply {
                setReferenceCounted(false)
                // 上限 6 分钟，比 thinking 模型的 callTimeout (360s) 略大；
                // 调用完成 stopService 会主动释放。
                acquire(360_000L)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "AI 后台保活",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "AI 调用期间显示一条小通知，避免熄屏被系统秒杀进程"
                        setShowBadge(false)
                    },
                )
            }
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Treasure")
            .setContentText("正在和 AI 对话…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "ai_keepalive"
        private const val NOTIFICATION_ID = 4101

        /** 起一发前台保活；调用 AI 之前调一次。重复调用安全（service 已起就忽略）。 */
        fun start(context: Context) {
            val intent = Intent(context, AiKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** AI 调用结束（成功 / 失败）后释放保活。 */
        fun stop(context: Context) {
            context.stopService(Intent(context, AiKeepAliveService::class.java))
        }
    }
}
