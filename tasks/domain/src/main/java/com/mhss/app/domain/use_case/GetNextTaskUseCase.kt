package com.mhss.app.domain.use_case

import com.mhss.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class GetNextTaskUseCase(
    private val repository: TaskRepository
) {
    /**
     * Hämtar den NÄSTA kommande tasken (t.ex. nästa medicin).
     * Tar första värdet från flödet, filtrerar bort passerade/klara,
     * och returnerar den med tidigast dueDate – eller null om ingen finns.
     */
    suspend operator fun invoke() =
        repository.getAllTasks()
            .first() // Flow<List<Task>> -> List<Task>
            .filter { it.dueDate > Instant.now().toEpochMilli() && !it.isCompleted }
            .minByOrNull { it.dueDate }
}
