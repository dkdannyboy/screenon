package com.dkdannyboy.screenon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Crash journal: persist the original value BEFORE changing a global setting. */
class TimeoutGuard(private val context: Context) {
    private val prefs = context.getSharedPreferences("timeout_guard", Context.MODE_PRIVATE)
    fun enabled(): Boolean = prefs.getBoolean("enabled", Build.MANUFACTURER.equals("samsung", true))
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean("enabled", value).commit() }
    fun needsPermission(): Boolean = enabled() && !Settings.System.canWrite(context)
    fun pendingRestore(): Boolean = prefs.contains("original")
    fun begin() {
        if (!enabled()) return
        check(Settings.System.canWrite(context)) { "호환 모드를 사용하려면 시스템 설정 변경을 허용해 주세요." }
        check(!pendingRestore()) { "이전 화면 꺼짐 설정을 먼저 복원해 주세요." }
        val original = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
        check(prefs.edit().putInt("original", original).commit()) { "원래 화면 꺼짐 시간을 저장하지 못했어요." }
        check(Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, KEEP_TIMEOUT)) { "화면 꺼짐 시간을 변경하지 못했어요." }
        check(current() == KEEP_TIMEOUT) { "기기 정책이 호환 설정을 제한하고 있어요." }
    }
    /** Do not overwrite a value the user or device policy changed during the session. */
    fun restore(): Boolean {
        if (!pendingRestore()) return true
        return try {
            if (current() == KEEP_TIMEOUT) {
                if (!Settings.System.canWrite(context)) return false
                if (!Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, prefs.getInt("original", 60000))) return false
                if (current() != prefs.getInt("original", 60000)) return false
            }
            prefs.edit().remove("original").commit()
        } catch (_: RuntimeException) { false }
    }
    fun current(): Int = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
    companion object {
        // Android has no public infinite-timeout setting. Retain the display wake lock as well.
        const val KEEP_TIMEOUT = Int.MAX_VALUE
        const val RESTORE_ERROR = "원래 화면 꺼짐 시간을 복원하지 못했어요. 시스템 설정 변경을 허용한 뒤 OFF를 눌러주세요."
    }
}

/** Only restores; never starts a screen-awake session after reboot or update. */
class TimeoutRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AwakeController.state.value.enabled &&
            (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)) {
            TimeoutGuard(context).restore()
            ScreenOnWidget.refresh(context)
        }
    }
}
