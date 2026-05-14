package com.treasure.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
        // Cycle 0034 v6：Android 14+ 强制 startForeground 显式带 serviceType；
        // 不传或值不匹配 manifest 里 declared 的会抛 SecurityException 并杀进程。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Treasure:ai-extract",
            ).apply {
                setReferenceCounted(false)
                // Cycle 0034 v6：上限 10 分钟。thinking 模型的 callTimeout 是
                // 360s，留出充足余量；用户主动 stop / 请求结束 stopService 会
                // 主动 release。
                acquire(600_000L)
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
                    // Cycle 0034 v6：importance 从 LOW 提到 DEFAULT。LOW 在
                    // 部分 OEM（vivo iManager / 华为 Magic）下被视为"可忽略"，
                    // 即便 startForeground 也可能不显示通知 → 系统当后台服务
                    // 秒杀。DEFAULT 强制下拉栏显示一条状态行。
                    NotificationChannel(
                        CHANNEL_ID,
                        "AI 后台保活",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "AI 调用期间显示一条小通知，避免熄屏被系统秒杀进程"
                        setShowBadge(false)
                        // 不响铃 / 不震动 — 这只是个"我在干活"标记。
                        setSound(null, null)
                        enableVibration(false)
                        enableLights(false)
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
            // Cycle 0034 v6：PRIORITY_DEFAULT 跟 channel importance 对齐。
            // foreground 服务在 vivo 上必须有"可见"通知才稳。
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
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
