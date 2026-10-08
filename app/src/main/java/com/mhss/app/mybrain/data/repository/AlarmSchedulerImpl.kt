package com.mhss.app.mybrain.data.repository

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.repository.AlarmScheduler
import com.mhss.app.alarm.use_case.GetAllAlarmsUseCase
import com.mhss.app.domain.use_case.GetNextTaskUseCase
import com.mhss.app.mybrain.AlarmServiceUpdater
import com.mhss.app.notification.AlarmReceiver
import com.mhss.app.notification.updateMedicineAlarmBadge
import com.mhss.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class AlarmSchedulerImpl(
    private val context: Context,
    private val getNextTaskUseCase: GetNextTaskUseCase,
    private val getAllAlarmsUseCase: GetAllAlarmsUseCase,
    private val alarmServiceUpdater: AlarmServiceUpdater
) : AlarmScheduler {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val tag = "AlarmScheduler"

    override fun scheduleAlarm(alarm: Alarm) {
        if (!canScheduleExactAlarms()) {
            Log.w(tag, "Kan inte schemalägga exact alarm – leder användare till inställningar")
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "Kunde inte öppna exact alarm-inställningar", e)
            }
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(Constants.TASK_ID_EXTRA, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            alarm.time,
            pendingIntent
        )

        Log.d(tag, "Alarm schemalagt → ID: ${alarm.id}, tid: ${alarm.time}, typ: ${alarm.type}")

        CoroutineScope(Dispatchers.IO).launch {
            // Foreground-notis
            val next = getNextTaskUseCase()
            if (next != null) {
                val medsText = next.subTasks.joinToString("\n") { sub -> "- ${sub.title}" }
                alarmServiceUpdater.updateForegroundNotification(
                    title = next.title,
                    description = medsText,
                    time = next.dueDate
                )
            }

            // Badge-uppdatering
            val alarms = getAllAlarmsUseCase()
            val now = System.currentTimeMillis()
            val activeCount = alarms.count { it.time > now }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.updateMedicineAlarmBadge(context, activeCount)
        }
    }

    override fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        Log.d(tag, "Alarm avbrutet → ID: $alarmId")

        CoroutineScope(Dispatchers.IO).launch {
            val next = getNextTaskUseCase()
            if (next != null) {
                val medsText = next.subTasks.joinToString("\n") { sub -> "- ${sub.title}" }
                alarmServiceUpdater.updateForegroundNotification(
                    title = next.title,
                    description = medsText,
                    time = next.dueDate
                )
            } else {
                alarmServiceUpdater.showDefaultForegroundNotification()
            }

            val alarms = getAllAlarmsUseCase()
            val now = System.currentTimeMillis()
            val activeCount = alarms.count { it.time > now }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.updateMedicineAlarmBadge(context, activeCount)
        }
    }

    override fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
    }
}
