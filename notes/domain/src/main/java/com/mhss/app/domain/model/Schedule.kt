package com.mhss.app.domain.model

/** En uppgift (t.ex. "Tillsyn") för en patient vid ett visst klockslag. */
data class ScheduleEntry(
    val noteId: Int,
    val patientName: String,
    val room: String,
    val task: String,
    val hour: Int,
    val folderId: Int = 0,
    val folderName: String = ""
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

    fun parse(note: Note, folderName: String = ""): List<ScheduleEntry> {
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
                .map { ScheduleEntry(note.id, name, room.orEmpty(), task, it, note.folderId ?: 0, folderName) }
        }
    }

    fun parseAll(notes: List<Note>, folderName: String = ""): List<ScheduleEntry> =
        notes.flatMap { parse(it, folderName) }

    private val ENTRY_ORDER = compareBy<ScheduleEntry> { it.roomNumber ?: Int.MAX_VALUE }
        .thenBy { it.room }
        .thenBy { it.patientName }
        .thenBy { it.task }

    /** Grupperar per klockslag (nattpassordning), sedan per avdelning (namn), sedan rum numeriskt. */
    fun groupByHourAndFolder(entries: List<ScheduleEntry>): List<HourGroup> =
        sortedHours(entries).map { hour ->
            val inHour = entries.filter { it.hour == hour }
            HourGroup(
                hour = hour,
                folders = inHour.groupBy { it.folderId to it.folderName }
                    .map { (key, rows) -> FolderGroup(key.first, key.second, rows.sortedWith(ENTRY_ORDER)) }
                    .sortedWith(compareBy<FolderGroup> { it.folderName.lowercase() }.thenBy { it.folderId })
            )
        }

    /** Nattpassordning: 18..23 före 00..17. */
    fun nightOrder(hour: Int): Int = (hour - 18 + 24) % 24

    fun sortedHours(entries: List<ScheduleEntry>): List<Int> =
        entries.map { it.hour }.distinct().sortedBy { nightOrder(it) }
}

data class FolderGroup(val folderId: Int, val folderName: String, val entries: List<ScheduleEntry>)

data class HourGroup(val hour: Int, val folders: List<FolderGroup>) {
    val entries: List<ScheduleEntry> get() = folders.flatMap { it.entries }
}
