package com.mhss.app.mybrain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mhss.app.mybrain.presentation.main.MainActivity
import com.mhss.app.ui.R as UiR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock

class MyForegroundService : Service() {

    private var isForegroundStarted = false
    private var nextTitle: String? = null
    private var nextDescription: String? = null
    private var nextTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            if (
                it.hasExtra("next_title") ||
                it.hasExtra("next_description") ||
                it.hasExtra("next_time")
            ) {
                nextTitle = it.getStringExtra("next_title")
                nextDescription = it.getStringExtra("next_description")
                nextTime = it.getLongExtra("next_time", 0L)
            }
        }

        val notification = buildNotification()

        if (!isForegroundStarted) {
            startForeground(1000001, notification)
            isForegroundStarted = true
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(1000001, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "my_service_channel",
                "VordNotes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false) // ⭐ viktigt: ingen badge från foreground-service
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title =
            nextTitle ?: getString(UiR.string.foreground_service_default_title)

        val timeFormatted =
            if (nextTime != 0L) formatTime(nextTime) else ""

        val collapsedText =
            if (timeFormatted.isNotEmpty()) "• $timeFormatted" else ""

        val expandedText = buildString {
            if (!nextDescription.isNullOrEmpty()) {
                append(nextDescription)
                append("\n")
            }
            if (timeFormatted.isNotEmpty()) {
                append("• $timeFormatted")
            }
        }.trim()

        return NotificationCompat.Builder(this, "my_service_channel")
            .setContentIntent(mainPendingIntent)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
            )
            .setSmallIcon(UiR.drawable.ic_medicine)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    override fun onTaskRemoved(rootIntent: Intent?) {

        val restartIntent = Intent(applicationContext, MyForegroundService::class.java).apply {
            setPackage(packageName)
        }

        val restartPendingIntent = PendingIntent.getService(
            this,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartPendingIntent
        )

        super.onTaskRemoved(rootIntent)
    }


}
