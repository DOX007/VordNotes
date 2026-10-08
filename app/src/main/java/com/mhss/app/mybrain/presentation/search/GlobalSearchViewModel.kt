package com.mhss.app.mybrain.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.domain.model.DiaryEntry
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.use_case.SearchEntriesUseCase
import com.mhss.app.domain.use_case.SearchNotesUseCase
import com.mhss.app.domain.use_case.SearchTasksUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel


@KoinViewModel
class GlobalSearchViewModel(
    private val searchNotesUseCase: SearchNotesUseCase,
    private val searchTasksUseCase: SearchTasksUseCase,
    private val searchEntriesUseCase: SearchEntriesUseCase
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val notes: List<Note> = emptyList(),
        val tasks: List<Task> = emptyList(),
        val entries: List<DiaryEntry> = emptyList()
    )

    var uiState by mutableStateOf(UiState())
        private set

    private var tasksJob: Job? = null

    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query)

        if (query.isBlank()) {
            // töm resultat och avbryt pågående flöden
            tasksJob?.cancel()
            uiState = uiState.copy(notes = emptyList(), tasks = emptyList(), entries = emptyList())
            return
        }

        // Notes (suspend -> lista)
        viewModelScope.launch {
            val notes = searchNotesUseCase(query)
            uiState = uiState.copy(notes = notes)
        }

        // Diary (suspend -> lista)
        viewModelScope.launch {
            val entries = searchEntriesUseCase(query)
            uiState = uiState.copy(entries = entries)
        }

        // Tasks (Flow -> kontinuerliga uppdateringar)
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            searchTasksUseCase(query).collectLatest { tasks ->
                uiState = uiState.copy(tasks = tasks)
            }
        }
    }
}

