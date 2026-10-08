package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Single

@Single
class AddCalendarEventUseCase(
    private val calendarEventRepository: CalendarRepository,
    private val widgetUpdater: WidgetUpdater
) {
    // Resultat som kan användas av UI:t för att visa feedback
    sealed class Result {
        object Success : Result()
        object NoCalendarAvailable : Result()   // saknad kalender/provider/behörighet
        data class Failure(val throwable: Throwable) : Result()
    }

    /**
     * NYTT: version som returnerar ett Result och kan visa meddelanden via notify-lambda.
     *
     * @param notify  valfri callback att visa text för användaren (t.ex. Toast/Snackbar).
     *                Ex: { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
     */
    suspend operator fun invoke(
        calendarEvent: CalendarEvent,
        notify: ((String) -> Unit)? = null
    ): Result {
        val calendars = runCatching { calendarEventRepository.getCalendars() }
            .getOrElse { emptyList() }

        var eventToAdd = calendarEvent

        if (calendars.isEmpty()) {
            // Försök skapa en lokal kalender och hämta igen
            runCatching { calendarEventRepository.createCalendar() }
            val afterCreate = runCatching { calendarEventRepository.getCalendars() }
                .getOrElse { emptyList() }

            val calendar = afterCreate.firstOrNull()
            if (calendar == null) {

                notify?.invoke("Ingen kalender hittades på enheten. Installera en kalenderapp eller ge kalenderbehörighet.")
                return Result.NoCalendarAvailable
            } else {
                eventToAdd = eventToAdd.copy(calendarId = calendar.id)
            }
        } else {
            // Välj befintlig kalender om eventet saknar giltig calendarId
            val selected = calendars.firstOrNull { it.id == calendarEvent.calendarId } ?: calendars.first()
            if (eventToAdd.calendarId != selected.id) {
                eventToAdd = eventToAdd.copy(calendarId = selected.id)
            }
        }

        val addRes = runCatching { calendarEventRepository.addEvent(eventToAdd) }
        // Uppdatera widgetar oavsett utfall (ska aldrig krascha)
        runCatching { widgetUpdater.updateAll(WidgetUpdater.WidgetType.Calendar) }

        return if (addRes.isSuccess) {
            Result.Success
        } else {
            notify?.invoke("Kunde inte skapa kalenderhändelsen.")
            Result.Failure(addRes.exceptionOrNull() ?: IllegalStateException("Okänt fel vid skapande av kalenderhändelse"))
        }
    }

    /**
     * GAMMAL signatur (bakåtkompatibel).
     * Anropar nya versionen men ignorerar resultatet och visar inget meddelande om du inte själv skickar in notify.
     */
    suspend operator fun invoke(calendarEvent: CalendarEvent) {
        invoke(calendarEvent, null)
    }
}