package com.mhss.app.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.ScheduleEntry
import com.mhss.app.domain.model.ScheduleParser
import com.mhss.app.domain.use_case.GetAllNoteFoldersUseCase
import com.mhss.app.domain.use_case.GetNotesByFolderUseCase
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.Order
import com.mhss.app.preferences.domain.model.intPreferencesKey
import com.mhss.app.preferences.domain.model.stringPreferencesKey
import com.mhss.app.preferences.domain.model.stringSetPreferencesKey
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.preferences.domain.use_case.SavePreferenceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.util.Calendar

enum class ScheduleResetMode(val value: Int) {
    SHIFT(0), DAY(1);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: SHIFT
    }
}

@KoinViewModel
class ScheduleViewModel(
    private val getFolderNotes: GetNotesByFolderUseCase,
    private val getAllFolders: GetAllNoteFoldersUseCase,
    private val getPreference: GetPreferenceUseCase,
    private val savePreference: SavePreferenceUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "ScheduleDebug"
        // Pass: ett nattpass räknas från kl 12:00 till 12:00 nästa dag (kvällen 18:00 och framåt
        // tillhör alltså samma pass som natten efter midnatt). Dygn: nollställs vid midnatt.
        private const val SHIFT_START_HOUR = 12

        fun currentPeriod(mode: ScheduleResetMode, now: Calendar = Calendar.getInstance()): String {
            val c = now.clone() as Calendar
            if (mode == ScheduleResetMode.SHIFT) c.add(Calendar.HOUR_OF_DAY, -SHIFT_START_HOUR)
            return "${mode.value}:${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        }
    }

    var uiState by mutableStateOf(UiState())
        private set

    private var loadJob: Job? = null
    private var tickerJob: Job? = null

    private val checkedKey = stringSetPreferencesKey(PrefsConstants.SCHEDULE_CHECKED_KEY)
    private val periodKey = stringPreferencesKey(PrefsConstants.SCHEDULE_PERIOD_KEY)
    private val modeKey = intPreferencesKey(PrefsConstants.SCHEDULE_RESET_MODE_KEY)

    data class UiState(
        val entries: List<ScheduleEntry> = emptyList(),
        val checked: Set<String> = emptySet(),
        val resetMode: ScheduleResetMode = ScheduleResetMode.SHIFT
    )

    fun load(folderIds: List<Int>) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            resetIfNewPeriod()

            val ids = folderIds.distinct()
            Log.d(TAG, "load() called with folderIds=$ids")

            val notesFlow: Flow<List<Pair<Int, List<Note>>>> =
                if (ids.isEmpty()) {
                    Log.d(TAG, "load: no folderIds (empty), emitting empty list")
                    flowOf(emptyList())
                } else {
                    combine(
                        ids.map { id -> getFolderNotes(id, Order.Manual) }
                    ) { lists -> ids.zip(lists) }
                }

            combine(
                notesFlow,
                getAllFolders(),
                getPreference(checkedKey, emptySet()),
                getPreference(modeKey, ScheduleResetMode.SHIFT.value)
            ) { notesPerFolder, folders, checked, mode ->
                val names = folders.associate { it.id to it.name }

                // Bygg parsed entries och logga detaljer per folder
                val parsedEntries = notesPerFolder.flatMap { (id, notes) ->
                    // Logga vilka notes som hämtats för denna folder
                    val noteTitles = notes.map { it.title }
                    Log.d(TAG, "folder=$id name=${names[id].orEmpty()} notesCount=${notes.size} titles=$noteTitles")

                    // Kör parsern för denna folders notes
                    val parsed = ScheduleParser.parseAll(notes.map { it.copy(folderId = id) }, names[id].orEmpty())

                    // Logga parsade entries för denna folder (inkl. info)
                    val parsedStrings = parsed.map { "${it.patientName}|${it.task}|${it.hour}|info=${it.info}" }
                    Log.d(TAG, "folder=$id parsedCount=${parsed.size} parsed=$parsedStrings")

                    parsed
                }

                Log.d(TAG, "total parsed entries count=${parsedEntries.size}")

                UiState(
                    entries = parsedEntries,
                    checked = checked,
                    resetMode = ScheduleResetMode.fromValue(mode)
                )
            }.onEach { uiState = it }.launchIn(this)
        }

        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                resetIfNewPeriod()
            }
        }
    }

    fun toggle(entry: ScheduleEntry, done: Boolean) {
        viewModelScope.launch {
            resetIfNewPeriod()
            val current = getPreference(checkedKey, emptySet()).first()
            savePreference(
                checkedKey,
                if (done) current + entry.checkKey else current - entry.checkKey
            )
        }
    }

    fun resetAll() {
        viewModelScope.launch { savePreference(checkedKey, emptySet()) }
    }

    fun setResetMode(mode: ScheduleResetMode) {
        viewModelScope.launch {
            savePreference(modeKey, mode.value)
            resetIfNewPeriod()
        }
    }

    // Nollställer avbockningar automatiskt när ett nytt pass/dygn har börjat.
    private suspend fun resetIfNewPeriod() {
        val mode = ScheduleResetMode.fromValue(getPreference(modeKey, ScheduleResetMode.SHIFT.value).first())
        val period = currentPeriod(mode)
        if (getPreference(periodKey, "").first() != period) {
            savePreference(checkedKey, emptySet())
            savePreference(periodKey, period)
            Log.d(TAG, "resetIfNewPeriod: period changed, cleared checked set and saved new period=$period")
        }
    }
}