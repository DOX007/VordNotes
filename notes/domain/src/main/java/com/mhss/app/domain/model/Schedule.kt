package com.mhss.app.domain.model

/** En uppgift (t.ex. "Tillsyn") för en patient vid ett visst klockslag. */
data class ScheduleEntry(
    val noteId: Int,
    val patientName: String,
    val room: String,
    val task: String,
    val hour: Int
) {
    /** Unik nyckel för avbockning: patient + klockslag + uppgift. */
    val checkKey: String get() = "$noteId|$hour|${task.lowercase()}"

    val roomNumber: Int? get() = ROOM_NUMBER.find(room)?.value?.toIntOrNull()

    private companion object {
        val ROOM_NUMBER = Regex("\\d+")
    }
}

object ScheduleParser {

    // "Tillsyn: 23- 02- 05", "Kamera: 22- 03", "Töm KAD: 05"
    private val TIME_LINE = Regex("""^([^:\d][^:]*?)\s*:\s*(\d{1,2}(?:\s*-\s*\d{1,2})*)\s*-?\s*$""")
    private val ROOM_LINE = Regex("""^rum(?:\s*:\s*|\s+)(.+)$""", RegexOption.IGNORE_CASE)
    private val TITLE_ROOM = Regex("""^(.*?)[\s,\-–]*(?:rum\s*)?(\d+\w?)$""", RegexOption.IGNORE_CASE)
    private val DECORATION = Regex("""^[\s>*_\-•]+|[*_\s]+$""")

    fun parse(note: Note): List<ScheduleEntry> {
        val lines = note.content.lines().map { it.replace(DECORATION, "") }

        var name = note.title.trim()
        var room = lines.firstNotNullOfOrNull { ROOM_LINE.matchEntire(it)?.groupValues?.get(1)?.trim() }
        if (room == null) {
            val m = TITLE_ROOM.matchEntire(name)
            if (m != null && m.groupValues[1].isNotBlank()) {
                name = m.groupValues[1].trim()
                room = m.groupValues[2]
            }
        }

        return lines.flatMap { line ->
            val m = TIME_LINE.matchEntire(line) ?: return@flatMap emptyList()
            val task = m.groupValues[1].trim()
            if (task.isEmpty() || ROOM_LINE.matches(line)) return@flatMap emptyList()
            m.groupValues[2].split("-")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 0..23 }
                .distinct()
                .map { ScheduleEntry(note.id, name, room.orEmpty(), task, it) }
        }
    }

    fun parseAll(notes: List<Note>): List<ScheduleEntry> = notes.flatMap { parse(it) }

    /** Nattpassordning: 18..23 före 00..17. */
    fun nightOrder(hour: Int): Int = (hour - 18 + 24) % 24

    fun sortedHours(entries: List<ScheduleEntry>): List<Int> =
        entries.map { it.hour }.distinct().sortedBy { nightOrder(it) }
}
