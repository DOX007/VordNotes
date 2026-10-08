package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.Note
import com.mhss.app.domain.repository.NoteRepository
import com.mhss.app.preferences.domain.model.Order
import com.mhss.app.preferences.domain.model.OrderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class GetAllFolderlessNotesUseCase(
    private val notesRepository: NoteRepository,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(order: Order): Flow<List<Note>> {
        return notesRepository.getAllFolderlessNotes().map { list ->
            when (order.orderType) {
                is OrderType.ASC -> {
                    // 🔹 Sortera enbart på skapelse-id (stabil ordning)
                    list.sortedBy { it.id }
                }
                is OrderType.DESC -> {
                    // 🔹 Samma fast omvänt
                    list.sortedByDescending { it.id }
                }
            }
        }.flowOn(defaultDispatcher)
    }
}
