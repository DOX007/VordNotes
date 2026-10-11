package com.mhss.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleParserTest {

    private val note = Note(
        id = 1,
        title = "Göte Nordman 3",
        content = "Tillsyn: 23- 02- 05\nKamera: 22- 03\nTöm KAD: 05\nUridom / förlängning\nVB"
    )

    @Test
    fun parsesHoursPerTask() {
        val entries = ScheduleParser.parse(note)
        assertEquals(listOf(23, 2, 5), entries.filter { it.task == "Tillsyn" }.map { it.hour })
        assertEquals(listOf(22, 3), entries.filter { it.task == "Kamera" }.map { it.hour })
        assertEquals(listOf(5), entries.filter { it.task == "Töm KAD" }.map { it.hour })
        assertEquals(6, entries.size)
    }

    @Test
    fun extractsNameAndRoom() {
        val e = ScheduleParser.parse(note).first()
        assertEquals("Göte Nordman", e.patientName)
        assertEquals(3, e.roomNumber)
    }

    @Test
    fun roomLineWins() {
        val e = ScheduleParser.parse(note.copy(title = "Anna", content = "Rum: 12\nTillsyn: 1")).single()
        assertEquals("Anna", e.patientName)
        assertEquals(12, e.roomNumber)
    }

    @Test
    fun nightOrdering() {
        val hours = listOf(3, 0, 23, 22, 6, 1).map { ScheduleEntry(1, "a", "1", "t", it) }
        assertEquals(listOf(22, 23, 0, 1, 3, 6), ScheduleParser.sortedHours(hours))
    }
}
