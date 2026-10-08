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
class GetNotesByFolderUseCase(
    private val notesRepository: NoteRepository,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(folderId: Int, order: Order): Flow<List<Note>> =
        notesRepository.getNotesByFolder(folderId).map { list ->
            when (order) {
                is Order.Manual -> {
                    list.sortedBy { it.orderIndex }
                }
                else -> {
                    val comparator = when (order.orderType) {
                        is OrderType.ASC -> compareBy<Note> { it.id }
                        is OrderType.DESC -> compareByDescending { it.id }
                    }
                    list.sortedWith(comparator)
                }
            }
        }.flowOn(defaultDispatcher)
}