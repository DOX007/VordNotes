package com.mhss.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.content.ContentUris
import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.model.AlarmType
import com.mhss.app.alarm.use_case.AddAlarmUseCase
import com.mhss.app.alarm.use_case.DeleteAlarmUseCase
import com.mhss.app.domain.model.TaskFrequency
import com.mhss.app.domain.use_case.GetTaskByIdUseCase
import com.mhss.app.domain.use_case.UpdateTaskUseCase
import com.mhss.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val deleteAlarmUseCase: DeleteAlarmUseCase by inject()
    private val addAlarmUseCase: AddAlarmUseCase by inject()
    private val getTaskByIdUseCase: GetTaskByIdUseCase by inject()
    private val updateTaskUseCase: UpdateTaskUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                val id = intent?.getIntExtra(Constants.TASK_ID_EXTRA, 0) ?: 0
                if (id == 0 || context == null) {
                    pendingResult.finish()
                    return@launch
                }

                // ---------- TASK / MEDICIN ----------
                val task = getTaskByIdUseCase(id)
                if (task != null) {

                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.sendNotification(task, context, id = 1)

                    val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(Constants.TASK_ID_EXTRA, task.id)
                    }
                    context.startActivity(alarmIntent)

                    updateTaskUseCase(
                        task.copy(lastFiredDate = task.dueDate),
                        false
                    )

                    if (task.recurring) {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = task.dueDate
                        }

                        when (task.frequency) {
                            TaskFrequency.EVERY_MINUTES ->
                                calendar.add(Calendar.MINUTE, task.frequencyAmount)
                            TaskFrequency.HOURLY ->
                                calendar.add(Calendar.HOUR, task.frequencyAmount)
                            TaskFrequency.DAILY ->
                                calendar.add(Calendar.DAY_OF_YEAR, task.frequencyAmount)
                            TaskFrequency.WEEKLY ->
                                calendar.add(Calendar.WEEK_OF_YEAR, task.frequencyAmount)
                            TaskFrequency.MONTHLY ->
                                calendar.add(Calendar.MONTH, task.frequencyAmount)
                            TaskFrequency.ANNUAL ->
                                calendar.add(Calendar.YEAR, task.frequencyAmount)
                        }

                        val nextTask = task.copy(dueDate = calendar.timeInMillis)

                        updateTaskUseCase(nextTask, false)

                        addAlarmUseCase(
                            Alarm(
                                id = nextTask.id,
                                time = nextTask.dueDate,
                                title = nextTask.title,
                                description = nextTask.description ?: "",
                                showOnLockScreen = true,
                                type = AlarmType.TASK
                            )
                        )
                    } else {
                        deleteAlarmUseCase(task.id)
                    }

                    pendingResult.finish()
                    return@launch
                }

                // ---------- KALENDER ----------
                val cr = context.contentResolver
                val eventUri =
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLong())

                cr.query(
                    eventUri,
                    arrayOf(
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DESCRIPTION,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.RRULE
                    ),
                    null,
                    null,
                    null
                )?.use { cur ->
                    if (cur.moveToFirst()) {
                        val eventId = cur.getLong(0)
                        val title = cur.getString(1) ?: ""
                        val desc = cur.getString(2) ?: ""
                        val rrule = cur.getString(5)

                        val manager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.sendCalendarNotification(
                            title = title.ifBlank {
                                context.getString(com.mhss.app.ui.R.string.app_name)
                            },
                            content = desc,
                            context = context,
                            eventId = eventId,
                            id = id
                        )

                        // 🔥 SAMMA BETEENDE SOM TASK
                        //val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                          //  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            //putExtra(Constants.TASK_ID_EXTRA, id)
                        //}
                        //context.startActivity(alarmIntent)

                        if (rrule.isNullOrBlank()) {
                            deleteAlarmUseCase(id)
                        }
                    } else {
                        deleteAlarmUseCase(id)
                    }
                }

                pendingResult.finish()
            } catch (_: Throwable) {
                pendingResult.finish()
            }
        }
    }
}
