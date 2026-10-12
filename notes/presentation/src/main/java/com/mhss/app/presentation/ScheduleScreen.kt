@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mhss.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mhss.app.domain.model.ScheduleEntry
import com.mhss.app.domain.model.ScheduleParser
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.MyBrainAppBar
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@Composable
fun ScheduleScreen(
    navController: NavHostController,
    folderIds: List<Int>,
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val state = viewModel.uiState
    var selectedTasks by remember { mutableStateOf(emptyList<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(folderIds) {
        viewModel.load(folderIds)
    }

    val taskTypes = state.entries
        .map { it.task }
        .distinctBy { it.lowercase() }
        .sorted()

    val filtered = state.entries.filter { entry ->
        selectedTasks.isEmpty() || selectedTasks.any { it.equals(entry.task, ignoreCase = true) }
    }

    val groups = ScheduleParser.groupByHourAndFolder(filtered)
    val hours = groups.map { it.hour }

    val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val nextHour = hours.indexOfFirst {
        ScheduleParser.nightOrder(it) >= ScheduleParser.nightOrder(nowHour)
    }.coerceAtLeast(0)

    LaunchedEffect(hours.size) {
        if (hours.isNotEmpty()) {
            listState.scrollToItem(nextHour)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            MyBrainAppBar(
                title = stringResource(R.string.schedule),
                actions = {
                    TextButton(onClick = { viewModel.resetAll() }) {
                        Text(stringResource(R.string.schedule_reset_all))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (taskTypes.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTasks.isEmpty(),
                        onClick = { selectedTasks = emptyList() },
                        label = { Text(stringResource(R.string.schedule_all)) }
                    )

                    taskTypes.forEach { task ->
                        val selected = selectedTasks.contains(task)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTasks =
                                    if (selected) selectedTasks - task else selectedTasks + task
                            },
                            label = { Text(task) }
                        )
                    }
                }
            }

            if (hours.isEmpty()) {
                Text(
                    text = stringResource(R.string.schedule_empty),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)
            ) {
                groups.forEach { group ->
                    val hour = group.hour
                    val rows = group.entries
                    item(key = "h$hour") {
                        val done = rows.count { it.checkKey in state.checked }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "%02d:00".format(hour),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (hours.indexOf(hour) == nextHour) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )

                            Text(
                                text = "$done/${rows.size}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    group.folders.forEach { folder ->
                        item(key = "f$hour-${folder.folderId}") {
                            Text(
                                text = folder.folderName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                            )
                        }
                        items(
                            items = folder.entries,
                            key = { entry -> "r$hour-${entry.checkKey}" }
                        ) { entry ->
                            val checked = entry.checkKey in state.checked
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (checked) 0.5f else 1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { viewModel.toggle(entry, it) }
                                )

                                val room = if (entry.room.isNotBlank()) {
                                    " – " + stringResource(R.string.schedule_room, entry.room)
                                } else {
                                    ""
                                }

                                Text(
                                    text = "${entry.patientName}$room – ${entry.task}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (checked) TextDecoration.LineThrough else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}