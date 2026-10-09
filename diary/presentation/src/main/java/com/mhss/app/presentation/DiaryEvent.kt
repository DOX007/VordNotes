package com.mhss.app.presentation

import com.mhss.app.preferences.domain.model.Order

sealed class DiaryEvent {
    data class SearchEntries(val query: String) : DiaryEvent()
    data class UpdateOrder(val order: Order) : DiaryEvent()
    data class ChangeChartEntriesRange(val monthly: Boolean) : DiaryEvent()
    data class SearchPatients(val query: String) : DiaryEvent()
    data object ClearPatientSuggestions : DiaryEvent()
    object ForceReload : DiaryEvent()
}