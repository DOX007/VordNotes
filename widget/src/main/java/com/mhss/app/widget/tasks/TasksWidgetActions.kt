package com.mhss.app.widget.tasks

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.preferences.domain.use_case.SavePreferenceUseCase
import com.mhss.app.util.Constants
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Öppnar Tasks-skärmen med "lägg till task"
 */
class AddTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "${Constants.TASKS_SCREEN_URI}?${Constants.ADD_TASK_ARG}=true".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}

/**
 * Navigerar till Tasks-skärmen
 */
class NavigateToTasksAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Constants.TASKS_SCREEN_URI.toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}

/**
 * Klick på en task i widgeten → öppna task-detaljer
 */
class TaskWidgetItemClickAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        parameters[taskId]?.let { id ->
            val intent = Intent(
                Intent.ACTION_VIEW,
                "${Constants.TASK_DETAILS_URI}/$id".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }
    }
}

/**
 * Checkbox i widgeten → markera task som klar/oklar
 */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        parameters[taskId]?.let { id ->
            parameters[completed]?.let { isCompleted ->
                val intent = Intent(context, CompleteTaskWidgetReceiver::class.java)
                intent.putExtra("taskId", id)
                intent.putExtra("completed", isCompleted)
                context.sendBroadcast(intent)
            }
        }
    }
}

/**
 * 👁-ikon i widget-header
 * NU: Läser nuvarande state, togglar det i preferences, SEN uppdaterar widgeten
 */
class ToggleShowCompletedTasksAction : ActionCallback, KoinComponent {

    private val getSettings: GetPreferenceUseCase by inject()
    private val saveSettings: SavePreferenceUseCase by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Läs nuvarande värde
        val currentShowCompleted = getSettings(
            booleanPreferencesKey(PrefsConstants.SHOW_COMPLETED_TASKS_KEY),
            false
        ).first()  // .first() för att få värdet synkront

        // Toggla och spara
        saveSettings(
            booleanPreferencesKey(PrefsConstants.SHOW_COMPLETED_TASKS_KEY),
            !currentShowCompleted
        )

        // Uppdatera widgeten (nu med nytt state)
        TasksWidget().updateAll(context)
    }
}

/**
 * Action-keys för widget-callbacks
 */
val taskId = ActionParameters.Key<Int>("taskId")
val completed = ActionParameters.Key<Boolean>("completed")