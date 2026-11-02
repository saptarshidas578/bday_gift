package com.pixelgift.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pixelgift.app.MainActivity
import com.pixelgift.app.R
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.BleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen App Widget providing quick Power on/off and Cherry Red scene controls.
 * Complies with SPEC 9.4 and 10.
 */
class PixelGiftWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_TOGGLE_POWER = "com.pixelgift.app.widget.ACTION_TOGGLE_POWER"
        const val ACTION_QUICK_CHERRY = "com.pixelgift.app.widget.ACTION_QUICK_CHERRY"
        private var isPowerOn = true
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_POWER -> {
                isPowerOn = !isPowerOn
                scope.launch {
                    try {
                        val transport = BleTransport(context)
                        val cmd = if (isPowerOn) {
                            Command.SetColor(210, 4, 45) // Turn on to Cherry Red
                        } else {
                            Command.SetMode(ProtocolConstants.MODE_OFF)
                        }
                        transport.send(cmd)
                    } catch (e: Exception) {
                        // Handled silently
                    }
                }
                updateAllWidgets(context)
            }
            ACTION_QUICK_CHERRY -> {
                isPowerOn = true
                scope.launch {
                    try {
                        val transport = BleTransport(context)
                        transport.send(Command.SetColor(210, 4, 45)) // Cherry Red
                    } catch (e: Exception) {
                        // Handled silently
                    }
                }
                updateAllWidgets(context)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, PixelGiftWidgetProvider::class.java))
        for (id in ids) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_pixelgift)

        // 1. Root click launches main activity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

        // 2. Power Toggle button
        val powerIntent = Intent(context, PixelGiftWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_POWER
        }
        val powerPendingIntent = PendingIntent.getBroadcast(
            context, 1, powerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_power, powerPendingIntent)
        views.setTextViewText(R.id.btn_widget_power, if (isPowerOn) "Turn Off" else "Turn On")

        // 3. Cherry Red quick button
        val cherryIntent = Intent(context, PixelGiftWidgetProvider::class.java).apply {
            action = ACTION_QUICK_CHERRY
        }
        val cherryPendingIntent = PendingIntent.getBroadcast(
            context, 2, cherryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_cherry, cherryPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

