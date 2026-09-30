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
            publish(context, AwakeState())
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
        ScreenOnWidget.refresh(this)
    }
}
