package com.mhss.app.mybrain

import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.core.annotation.Single

@Single
class AlarmServiceUpdater(
    private val context: Context
) {

    /**
     * Uppdaterar foreground-notisen med nästa alarm
     */
    fun updateForegroundNotification(
        title: String,
        description: String,
        time: Long
    ) {
        val intent = Intent(context, MyForegroundService::class.java).apply {
            putExtra("next_title", title)
            putExtra("next_description", description)
            putExtra("next_time", time)
        }

        startServiceCompat(intent)
    }

    /**
     * Visar default-foreground-notis (foreground_service_default_title).
     * Används när:
     * - alla alarm är avklarade
     * - ett framtida alarm öppnas/redigeras och state måste nollställas
     */
    fun showDefaultForegroundNotification() {
        val intent = Intent(context, MyForegroundService::class.java).apply {
            // EXPLICIT reset av servicens state
            putExtra("next_title", null as String?)
            putExtra("next_description", null as String?)
            putExtra("next_time", 0L)
        }

        startServiceCompat(intent)
    }

    /**
     * Startar service korrekt på alla Android-versioner
     */
    private fun startServiceCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
