package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.preferences.domain.model.Order
import com.mhss.app.preferences.domain.model.OrderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class GetAllTasksUseCase(
    private val tasksRepository: TaskRepository,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(
        order: Order,
        showCompleted: Boolean = true
    ): Flow<List<Task>> {
        return tasksRepository.getAllTasks().map { tasks ->
            val filtered = if (showCompleted) tasks else tasks.filter { !it.isCompleted }

            when (order) {
                is Order.Manual -> {
                    // Manuell sortering – använder orderIndex
                    filtered.sortedBy { it.orderIndex }
                }

                else -> {
                    // Befintliga sorteringsalternativ
                    when (order.orderType) {
                        is OrderType.ASC -> {
                            when (order) {
                                is Order.Alphabetical -> filtered.sortedBy { it.title }
                                is Order.DateCreated -> filtered.sortedBy { it.createdDate }
                                is Order.DateModified -> filtered.sortedBy { it.updatedDate }
                                is Order.Priority -> filtered.sortedBy { it.priority }
                                is Order.DueDate -> filtered.sortedWith(
                                    compareBy({ it.dueDate == 0L }, { it.dueDate })
                                )
                                else -> filtered.sortedBy { it.updatedDate } // fallback
                            }
                        }

                        is OrderType.DESC -> {
                            when (order) {
                                is Order.Alphabetical -> filtered.sortedByDescending { it.title }
                                is Order.DateCreated -> filtered.sortedByDescending { it.createdDate }
                                is Order.DateModified -> filtered.sortedByDescending { it.updatedDate }
                                is Order.Priority -> filtered.sortedByDescending { it.priority }
                                is Order.DueDate -> filtered.sortedWith(
                                    compareBy({ it.dueDate == 0L }, { it.dueDate })
                                ).reversed()
                                else -> filtered.sortedByDescending { it.updatedDate } // fallback
                            }
                        }
                    }
                }
            }
        }.flowOn(defaultDispatcher)
    }
}