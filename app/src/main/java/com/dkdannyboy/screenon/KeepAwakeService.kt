package com.dkdannyboy.screenon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock

/** No overlay and no settings mutation. A display wake lock survives switching apps. */
class KeepAwakeService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var receiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) stopSession()
        }
    }
    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOffReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenOffReceiver, filter)
        receiverRegistered = true
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "화면 켜짐 유지", NotificationManager.IMPORTANCE_LOW).apply {
                description = "화면 켜짐 유지가 실행되는 동안 표시되는 조용한 알림"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }
    @Suppress("DEPRECATION") // FLAG_KEEP_SCREEN_ON only works in our Activity; cross-app is the product.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != START) {
            stopSession()
            return START_NOT_STICKY
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else startForeground(NOTIFICATION_ID, notification())
            val power = getSystemService(PowerManager::class.java)
            if (!power.isInteractive || !power.isWakeLockLevelSupported(PowerManager.SCREEN_BRIGHT_WAKE_LOCK)) {
                stopSession("이 기기에서는 지금 화면 켜짐 유지를 시작할 수 없어요.")
                return START_NOT_STICKY
            }
            if (wakeLock?.isHeld != true) {
                wakeLock = power.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ScreenOn:display").apply {
                    setReferenceCounted(false)
                    // Explicit ON means no timer. OFF, lock screen, or process death releases it.
                    acquire()
                }
                AwakeController.publish(this, AwakeState(true, SystemClock.elapsedRealtime()))
            }
        } catch (_: RuntimeException) {
            stopSession("화면 켜짐 유지를 시작하지 못했어요. 다시 시도해 주세요.")
        }
        // Never unexpectedly turn ON again after a kill/reboot/force-stop.
        return START_NOT_STICKY
    }
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val off = PendingIntent.getService(this, 1, Intent(this, KeepAwakeService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_power)
            .setContentTitle("ScreenOn · ON")
            .setContentText("화면 켜짐 유지 중 · 눌러서 앱 열기")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "끄기", off).build())
            .setOngoing(true).setOnlyAlertOnce(true)
            .apply { if (Build.VERSION.SDK_INT >= 31) setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) }
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false).build()
    }
    private fun stopSession(error: String? = null) {
        releaseLock()
        AwakeController.publish(this, AwakeState(error = error))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun releaseLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
    override fun onDestroy() {
        releaseLock()
        if (receiverRegistered) unregisterReceiver(screenOffReceiver)
        // Preserve any actionable startup error until the next user action.
        AwakeController.publish(this, AwakeController.state.value.copy(enabled = false, startedAt = 0))
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val START = "com.dkdannyboy.screenon.START"
        const val STOP = "com.dkdannyboy.screenon.STOP"
        private const val CHANNEL = "screen_awake"
        private const val NOTIFICATION_ID = 1
    }
}
