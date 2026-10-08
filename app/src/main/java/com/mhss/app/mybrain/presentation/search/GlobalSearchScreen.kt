package com.mhss.app.mybrain.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mhss.app.ui.R
import com.mhss.app.ui.components.notes.NoteCard
import com.mhss.app.ui.components.tasks.TaskCard
import com.mhss.app.ui.navigation.Screen
import com.mhss.app.presentation.DiaryEntryItem
import com.mhss.app.ui.components.common.TTSSpeakerIcon
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextAlign


@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    viewModel: GlobalSearchViewModel = koinViewModel()
) {
    val state = viewModel.uiState

    var query by rememberSaveable { mutableStateOf("") }
    var playingKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        playingKey = null
        viewModel.onQueryChange(query)
    }

    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
    ) {

        item {

            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                withFrameNanos { }
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search)) },
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        // ----- NOTES -----
        if (state.notes.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.notes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(state.notes, key = { note -> "note-${note.id}" }) { note ->
                val key = "note-${note.id}"

                Box {
                    NoteCard(
                        note = note,
                        onClick = {
                            playingKey = null
                            navController.navigate(
                                Screen.NoteDetailsScreen(noteId = note.id)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TTSSpeakerIcon(
                        text = buildString {
                            append(note.title)
                            if (note.content.isNotBlank()) {
                                append(". ")
                                append(note.content)
                            }
                        },
                        isPlaying = playingKey == key,
                        onPlayPauseClick = {
                            playingKey = if (playingKey == key) null else key
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
            }
        }

        // ----- TASKS -----
        if (state.tasks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tasks),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(state.tasks, key = { task -> "task-${task.id}" }) { task ->
                val key = "task-${task.id}"

                Box {
                    TaskCard(
                        task = task,
                        onComplete = {},
                        onClick = {
                            playingKey = null
                            navController.navigate(
                                Screen.TaskDetailScreen(taskId = task.id)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TTSSpeakerIcon(
                        text = buildString {
                            append(task.title)
                            if (task.description.isNotBlank()) {
                                append(". ")
                                append(task.description)
                            }
                        },
                        isPlaying = playingKey == key,
                        onPlayPauseClick = {
                            playingKey = if (playingKey == key) null else key
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    )
                }
            }
        }

        // ----- DIARY -----
        if (state.entries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.diary),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(state.entries, key = { entry -> "diary-${entry.id}" }) { entry ->
                val key = "diary-${entry.id}"

                Box {
                    DiaryEntryItem(
                        entry = entry,
                        isPlaying = playingKey == key,
                        onPlayPauseClick = {},
                        onClick = { diaryEntry ->
                            playingKey = null
                            navController.navigate(
                                Screen.DiaryDetailScreen(entryId = diaryEntry.id)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TTSSpeakerIcon(
                        text = buildString {
                            append(entry.title)
                            if (entry.content.isNotBlank()) {
                                append(". ")
                                append(entry.content)
                            }
                        },
                        isPlaying = playingKey == key,
                        onPlayPauseClick = {
                            playingKey = if (playingKey == key) null else key
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
            }
        }

        // ----- EMPTY STATE -----
        if (
            query.isBlank() &&
            state.notes.isEmpty() &&
            state.tasks.isEmpty() &&
            state.entries.isEmpty()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.search_icon2),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp)
                        )

                        Text(
                            text = stringResource(R.string.search_for_content),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
