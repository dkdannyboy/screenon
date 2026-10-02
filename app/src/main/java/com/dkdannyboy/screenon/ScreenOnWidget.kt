package com.dkdannyboy.screenon

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ScreenOnWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            TURN_ON -> AwakeController.setEnabled(context, true)
            TURN_OFF -> AwakeController.setEnabled(context, false)
        }
    }
    companion object {
        private const val TURN_ON = "com.dkdannyboy.screenon.WIDGET_ON"
        private const val TURN_OFF = "com.dkdannyboy.screenon.WIDGET_OFF"
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScreenOnWidget::class.java))
            if (ids.isEmpty()) return
            val enabled = AwakeController.state.value.enabled
            val views = RemoteViews(context.packageName, R.layout.widget_screenon).apply {
                setTextViewText(R.id.widget_toggle, if (enabled) "ON" else "OFF")
                setTextViewText(R.id.widget_status, if (enabled) "화면 유지 중" else if (TimeoutGuard(context).needsPermission()) "호환 설정 필요" else "유지 꺼짐")
                setContentDescription(R.id.widget_toggle, if (enabled) "화면 켜짐 유지 끄기" else "화면 켜짐 유지 켜기")
                setOnClickPendingIntent(R.id.widget_toggle, if (!enabled && TimeoutGuard(context).needsPermission()) PendingIntent.getActivity(context, 4,
                    Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) else PendingIntent.getBroadcast(context, 2,
                    Intent(context, ScreenOnWidget::class.java).setAction(if (enabled) TURN_OFF else TURN_ON), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                setOnClickPendingIntent(R.id.widget_open, PendingIntent.getActivity(context, 3,
                    Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
