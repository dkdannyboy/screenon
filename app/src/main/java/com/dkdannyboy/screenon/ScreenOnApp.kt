package com.dkdannyboy.screenon

import android.app.Application
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AwakeState(val enabled: Boolean = false, val startedAt: Long = 0, val error: String? = null)

/** Runtime truth, deliberately not a persisted ON preference. A dead process owns no lock. */
object AwakeController {
    private val mutable = MutableStateFlow(AwakeState())
    val state = mutable.asStateFlow()
    internal fun publish(context: Context, value: AwakeState) {
        mutable.value = value
        ScreenOnWidget.refresh(context)
    }
    fun setEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            context.stopService(Intent(context, KeepAwakeService::class.java))
            val restored = TimeoutGuard(context).restore()
            publish(context, AwakeState(error = if (restored) null else TimeoutGuard.RESTORE_ERROR))
            return
        }
        if (TimeoutGuard(context).needsPermission()) {
            publish(context, AwakeState(error = "호환 모드 설정에서 시스템 설정 변경을 허용해 주세요."))
            return
        }
        try {
            context.startForegroundService(Intent(context, KeepAwakeService::class.java).setAction(KeepAwakeService.START))
        } catch (_: RuntimeException) {
            publish(context, AwakeState(error = "실행하지 못했어요. 앱을 연 상태에서 다시 켜주세요."))
        }
    }
}

class ScreenOnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Also repairs stale launcher widgets after process death or a device reboot.
        val restored = TimeoutGuard(this).restore()
        AwakeController.publish(this, AwakeState(error = if (restored) null else TimeoutGuard.RESTORE_ERROR))
    }
}
