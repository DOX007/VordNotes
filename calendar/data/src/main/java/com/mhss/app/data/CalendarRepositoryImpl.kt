package com.mhss.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.ui.R
import com.mhss.app.domain.model.Calendar
import com.mhss.app.domain.model.CalendarEventFrequency
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.TimeZone
import android.provider.CalendarContract.Calendars
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Single
class CalendarRepositoryImpl(
    private val context: Context,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : CalendarRepository {

    // ---------- Säkerhetskontroller ----------
    private fun hasCalendarProvider(): Boolean =
        context.packageManager.resolveContentProvider(CalendarContract.AUTHORITY, 0) != null

    private fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    private fun canRead(): Boolean = hasCalendarProvider() && hasReadPermission()
    private fun canWrite(): Boolean = hasCalendarProvider() && hasWritePermission()

    // ---------- Events ----------

    override suspend fun getEvents(): List<CalendarEvent> = withContext(ioDispatcher) {
        if (!canRead()) return@withContext emptyList()

        val projection: Array<String> = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_COLOR,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.CALENDAR_COLOR
        )
        val instancesProjection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.RRULE,
            CalendarContract.Instances.DURATION,
            CalendarContract.Instances.CALENDAR_COLOR
        )

        val contentResolver = context.contentResolver
        val events = mutableListOf<CalendarEvent>()

        // Enstaka (icke-återkommande) events
        runCatching {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                "${CalendarContract.Events.DELETED} = 0",
                null,
                null
            )
        }.getOrNull()?.use { cur ->
            while (cur.moveToNext()) {
                val eventId: Long = cur.getLong(ID_INDEX)
                val title: String = cur.getString(TITLE_INDEX) ?: continue
                val description: String? = cur.getString(DESC_INDEX)
                val start: Long = cur.getLong(START_INDEX)
                val duration: String = cur.getString(EVENT_DURATION_INDEX) ?: ""
                val end: Long = when {
                    duration.isNotBlank() -> duration.extractEndFromDuration(start)
                    cur.getLong(END_INDEX) > 0 -> cur.getLong(END_INDEX)
                    else -> start
                }
                val location: String? = cur.getString(LOCATION_INDEX)
                val allDay: Boolean = cur.getInt(ALL_DAY_INDEX) == 1
                val color: Int = cur.getInt(COLOR_INDEX)
                val calendarColor: Int = cur.getInt(CALENDAR_COLOR_INDEX)
                val calendarId: Long = cur.getLong(EVENT_CALENDAR_ID_INDEX)
                val rrule: String = cur.getString(EVENT_RRULE_INDEX) ?: ""
                val recurring: Boolean = rrule.isNotBlank()

                if (!recurring) {
                    events += CalendarEvent(
                        id = eventId,
                        title = title,
                        description = description,
                        start = start,
                        end = end,
                        location = location,
                        allDay = allDay,
                        color = if (color != 0) color else calendarColor,
                        calendarId = calendarId,
                    )
                }
            }
        }

        // Återkommande events via Instances
        val startM = 0L
        val endM = System.currentTimeMillis()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startM)
        ContentUris.appendId(builder, endM)

        runCatching {
            contentResolver.query(builder.build(), instancesProjection, null, null, null)
        }.getOrNull()?.use { curI ->
            while (curI.moveToNext()) {
                val eventId: Long = curI.getLong(ID_INDEX)
                val title: String = curI.getString(TITLE_INDEX) ?: continue
                val description: String? = curI.getString(DESC_INDEX)
                val start: Long = curI.getLong(START_INDEX)
                val duration: String = curI.getString(EVENT_DURATION_INDEX) ?: ""
                val end: Long = when {
                    duration.isNotBlank() -> duration.extractEndFromDuration(start)
                    curI.getLong(END_INDEX) > 0 -> curI.getLong(END_INDEX)
                    else -> start
                }
                val location: String? = curI.getString(LOCATION_INDEX)
                val allDay: Boolean = curI.getInt(ALL_DAY_INDEX) == 1
                val color: Int = curI.getInt(COLOR_INDEX)
                val calendarColor: Int = curI.getInt(CALENDAR_COLOR_INDEX)
                val calendarId: Long = curI.getLong(EVENT_CALENDAR_ID_INDEX)
                val rrule: String = curI.getString(EVENT_RRULE_INDEX) ?: ""
                val recurring: Boolean = rrule.isNotBlank()
                val frequency: CalendarEventFrequency = rrule.extractFrequency()

                if (recurring) {
                    events += CalendarEvent(
                        id = eventId,
                        title = title,
                        description = description,
                        start = start,
                        end = end,
                        location = location,
                        allDay = allDay,
                        color = if (color != 0) color else calendarColor,
                        calendarId = calendarId,
                        frequency = frequency,
                        recurring = true,
                    )
                }
            }
        }

        events.sortedByDescending { it.start }
    }

    // ---------- Kalendrar ----------

    override suspend fun getCalendars(): List<Calendar> = withContext(ioDispatcher) {
        if (!canRead()) return@withContext emptyList()

        val cr = context.contentResolver
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.CALENDAR_COLOR,
            Calendars.VISIBLE
        )
        val selection = "${Calendars.VISIBLE} = ?"
        val selectionArgs = arrayOf("1")
        val sortOrder = "${Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC"

        runCatching {
            cr.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, sortOrder)
        }.getOrNull()?.use { cur ->
            val idIdx = cur.getColumnIndexOrThrow(Calendars._ID)
            val nameIdx = cur.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME)
            val accIdx = cur.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME)
            val colorIdx = cur.getColumnIndex(Calendars.CALENDAR_COLOR)

            val calendars = mutableListOf<Calendar>()
            while (cur.moveToNext()) {
                val id = cur.getLong(idIdx)
                val name = cur.getString(nameIdx) ?: ""
                val acc = cur.getString(accIdx) ?: ""
                val color = if (colorIdx >= 0) cur.getInt(colorIdx) else 0

                calendars += Calendar(
                    id,
                    name,
                    acc,
                    color
                )
            }
            calendars
        } ?: emptyList()
    }

    // ---------- CRUD ----------

    override suspend fun addEvent(event: CalendarEvent) {
        withContext(ioDispatcher) {
            if (!canWrite()) return@withContext

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.start)
                put(CalendarContract.Events.ALL_DAY, event.allDay)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                if (event.recurring) {
                    put(CalendarContract.Events.RRULE, event.getEventRRule())
                    put(CalendarContract.Events.DURATION, event.getEventDuration())
                } else {
                    put(CalendarContract.Events.DTEND, event.end)
                }
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val insertedUri = runCatching {
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }.getOrNull() ?: return@withContext

            val insertedEventId = runCatching {
                ContentUris.parseId(insertedUri)
            }.getOrNull() ?: return@withContext

            // 🔔 Skapa system-reminder kopplat till kalender-eventet
            val reminderMinutes = 0

            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, insertedEventId)
                put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                put(
                    CalendarContract.Reminders.METHOD,
                    CalendarContract.Reminders.METHOD_ALERT
                )
            }

            context.contentResolver.insert(
                CalendarContract.Reminders.CONTENT_URI,
                reminderValues
            )
                    }
    }


    override suspend fun updateEvent(event: CalendarEvent) {
        withContext(ioDispatcher) {
            if (!canWrite()) return@withContext

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.start)
                if (event.recurring && event.frequency != CalendarEventFrequency.NEVER) {
                    val end: Long? = null
                    put(CalendarContract.Events.RRULE, event.getEventRRule())
                    put(CalendarContract.Events.DURATION, event.getEventDuration())
                    put(CalendarContract.Events.DTEND, end)
                } else {
                    val rule: String? = null
                    put(CalendarContract.Events.RRULE, rule)
                    put(CalendarContract.Events.DURATION, rule)
                    put(CalendarContract.Events.DTEND, event.end)
                }
                put(CalendarContract.Events.ALL_DAY, event.allDay)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
            }

            val updateUri: Uri =
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)

            val updated = runCatching {
                context.contentResolver.update(updateUri, values, null, null)
            }.getOrNull() ?: 0

            if (updated > 0) {

                // 1️⃣ Ta bort gamla reminders för eventet
                context.contentResolver.delete(
                    CalendarContract.Reminders.CONTENT_URI,
                    "${CalendarContract.Reminders.EVENT_ID} = ?",
                    arrayOf(event.id.toString())
                )

                // 2️⃣ Skapa ny reminder kopplad till eventet
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, event.id)
                    put(CalendarContract.Reminders.MINUTES, 0)
                    put(
                        CalendarContract.Reminders.METHOD,
                        CalendarContract.Reminders.METHOD_ALERT
                    )
                }

                context.contentResolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    reminderValues
                )
            }
        }
    }

    override suspend fun createCalendar() {
        withContext(ioDispatcher) {
            if (!canWrite()) return@withContext

            val uri = CalendarContract.Calendars.CONTENT_URI.asSyncAdapter(
                context.getString(R.string.app_name),
                CalendarContract.ACCOUNT_TYPE_LOCAL
            )
            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, context.getString(R.string.app_name))
                put(CalendarContract.Calendars.NAME, context.getString(R.string.app_name))
                put(CalendarContract.Calendars.ACCOUNT_NAME, context.getString(R.string.app_name))
                put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                put(CalendarContract.Calendars.CALENDAR_COLOR, 0x03DAC5)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            }

            runCatching {
                context.contentResolver.insert(uri, values)
            }
        }
    }

    override suspend fun deleteEvent(event: CalendarEvent) {
        withContext(ioDispatcher) {
            if (!canWrite()) return@withContext

            val updateUri: Uri =
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)

            val deleted = runCatching {
                context.contentResolver.delete(updateUri, null, null)
            }.getOrNull() ?: 0

            if (deleted > 0) {

            }
        }
    }

    // ---------- Hjälpare ----------

    private fun Uri.asSyncAdapter(account: String, accountType: String): Uri {
        return buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build()
    }

    private fun String.extractFrequency(): CalendarEventFrequency {
        return if (this.contains("FREQ=")) {
            val freq = this.substringAfter("FREQ=").substringBefore(";")
            when (freq) {
                "DAILY" -> CalendarEventFrequency.DAILY
                "WEEKLY" -> CalendarEventFrequency.WEEKLY
                "MONTHLY" -> CalendarEventFrequency.MONTHLY
                "YEARLY" -> CalendarEventFrequency.YEARLY
                else -> CalendarEventFrequency.NEVER
            }
        } else CalendarEventFrequency.NEVER
    }

    private fun CalendarEvent.getEventDuration(): String {
        return "P${(end - start) / 1000}S"
    }

    private fun String.extractEndFromDuration(start: Long): Long {
        return try {
            val duration = this.substring(1, this.length - 1).toLong() * 1000
            start + duration
        } catch (e: Exception) {
            start
        }
    }

    private fun CalendarEvent.getEventRRule(): String {
        return buildString { append("FREQ=$frequency") }
    }

    companion object {
        private const val ID_INDEX = 0
        private const val TITLE_INDEX = 1
        private const val DESC_INDEX = 2
        private const val START_INDEX = 3
        private const val END_INDEX = 4
        private const val LOCATION_INDEX = 5
        private const val ALL_DAY_INDEX = 6
        private const val COLOR_INDEX = 7
        private const val EVENT_CALENDAR_ID_INDEX = 8
        private const val EVENT_RRULE_INDEX = 9
        private const val EVENT_DURATION_INDEX = 10
        private const val CALENDAR_COLOR_INDEX = 11

        private const val CALENDAR_ID_INDEX: Int = 0
        private const val CALENDAR_NAME_INDEX: Int = 1
        private const val ACCOUNT_NAME_INDEX: Int = 2
        private const val CALENDAR_CALENDAR_COLOR_INDEX: Int = 4
    }
}