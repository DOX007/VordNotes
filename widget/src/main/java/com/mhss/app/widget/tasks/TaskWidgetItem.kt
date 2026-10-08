package com.mhss.app.widget.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mhss.app.domain.model.Priority
import com.mhss.app.domain.model.Task
import com.mhss.app.ui.R
import com.mhss.app.ui.color
import com.mhss.app.util.date.formatDateDependingOnDay
import com.mhss.app.util.date.isDueDateOverdue
import com.mhss.app.widget.smallBackgroundBasedOnVersion


@Composable
fun TaskWidgetItem(
    task: Task
) {
    val context = LocalContext.current

    Box(
        modifier = GlanceModifier.padding(bottom = 3.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .smallBackgroundBasedOnVersion()
                .padding(10.dp)
        ) {
            // Row 1: Checkbox + Title + Due date/clock
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskWidgetCheckBox(
                    isComplete = task.isCompleted,
                    borderColor = task.priority.color,
                    onComplete = actionRunCallback<CompleteTaskAction>(
                        parameters = actionParametersOf(
                            taskId to task.id,
                            completed to !task.isCompleted
                        )
                    )
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = task.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 2,
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<TaskWidgetItemClickAction>(
                                parameters = actionParametersOf(taskId to task.id)
                            )
                        )
                    )

                    if (task.isCompleted) {
                        Spacer(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(ColorProvider(Color(0xFFE53935)))
                        )
                    }
                }

                if (task.dueDate != 0L) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_medicine),
                        modifier = GlanceModifier.size(12.dp),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            if (task.dueDate.isDueDateOverdue()) ColorProvider(Color.Red)
                            else GlanceTheme.colors.onSecondaryContainer
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Text(
                        text = task.dueDate.formatDateDependingOnDay(context),
                        style = TextStyle(
                            color = if (task.dueDate.isDueDateOverdue()) ColorProvider(Color.Red)
                            else GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Row 2: Subtasks progress + titles
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.subTasks.isNotEmpty()) {
                    val total = task.subTasks.size
                    val done = task.subTasks.count { it.isCompleted }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_bullet_list),
                            modifier = GlanceModifier.size(12.dp),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Text(
                            text = "$done/$total",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }

                if (task.subTasks.isNotEmpty()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        task.subTasks.forEach { sub ->
                            Box(
                                modifier = GlanceModifier.padding(end = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sub.title,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSecondaryContainer,
                                        fontSize = 12.sp
                                    )
                                )

                                if (sub.isCompleted) {
                                    Spacer(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(ColorProvider(Color(0xFFE53935)))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskWidgetCheckBox(
    isComplete: Boolean,
    borderColor: Color,
    onComplete: Action
) {
    Box(
        modifier = GlanceModifier
            .size(25.dp)
            .background(
                ImageProvider(
                    when (borderColor) {
                        Priority.LOW.color    -> R.drawable.task_check_box_background_green
                        Priority.MEDIUM.color -> R.drawable.task_check_box_background_orange
                        else                  -> R.drawable.task_check_box_background_red
                    }
                )
            )
            .clickable(onClick = onComplete)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isComplete) {
            Image(
                provider = ImageProvider(R.drawable.ic_check),
                modifier = GlanceModifier.size(14.dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
            )
        }
    }
}