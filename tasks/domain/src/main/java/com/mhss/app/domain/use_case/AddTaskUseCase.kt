package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.model.AlarmType
import com.mhss.app.alarm.use_case.AddAlarmUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Single

@Single
class AddTaskUseCase(
    private val tasksRepository: TaskRepository,
    private val addAlarm: AddAlarmUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(task: Task): Boolean {
        val id = tasksRepository.insertTask(task).toInt()

        // Uppdatera widgets som tidigare
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)

        return if (task.dueDate != 0L) {
            val success = addAlarm(
                Alarm(
                    id = id,
                    time = task.dueDate,
                    title = task.title,
                    description = task.description ?: "",
                    showOnLockScreen = true,
                    type = AlarmType.TASK
                )
            )

            if (!success) {
                // Samma beteende som innan:
                // ta bort dueDate om alarmet inte kan skapas
                updateTask(
                    task.copy(
                        id = id,
                        dueDate = 0L
                    ),
                    false
                )
            }

            success
        } else {
            true
        }
    }
}
