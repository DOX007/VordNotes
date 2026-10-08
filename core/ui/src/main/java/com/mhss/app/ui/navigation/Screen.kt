package com.mhss.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Main : Screen()

    @Serializable
    data object SpacesScreen : Screen()

    @Serializable
    data object DashboardScreen : Screen()

    @Serializable
    data object SettingsScreen : Screen()

    @Serializable
    data class TasksScreen(
        val addTask: Boolean = false,
        val prefillTitle: String = ""
    ) : Screen()

    @Serializable
    data class TaskDetailScreen(
        val taskId: Int
    ) : Screen()

    @Serializable
    data object TaskSearchScreen : Screen()

    @Serializable
    data class NotesScreen(
        val addNote: Boolean = false,
        val prefillNoteTitle: String = ""
    ) : Screen()

    @Serializable
    data class NoteDetailsScreen(
        val noteId: Int = -1,
        val folderId: Int = -1
    ) : Screen()

    @Serializable
    data object NoteSearchScreen : Screen()

    @Serializable
    data object DiaryScreen : Screen()

    @Serializable
    data class DiaryDetailScreen(
        val entryId: Int = -1
    ) : Screen()

    @Serializable
    data object DiarySearchScreen : Screen()

    @Serializable
    data object GlobalSearchScreen : Screen()

    @Serializable
    data object DiaryChartScreen : Screen()

    @Serializable
    data object BookmarksScreen : Screen()

    @Serializable
    data class BookmarkDetailScreen(
        val bookmarkId: Int = -1
    ) : Screen()

    @Serializable
    data object BookmarkSearchScreen : Screen()

    @Serializable
    data object CalendarScreen : Screen()

    @Serializable
    data class CalendarEventDetailsScreen(
        val eventJson: String? = null
    ) : Screen()

    @Serializable
    data class NoteFolderDetailsScreen(
        val folderId: Int
    ) : Screen()

    @Serializable
    data object ImportExportScreen : Screen()

    @Serializable
    data object PermissionsScreen : Screen()

    @Serializable
    data object IntegrationsScreen : Screen()

    @Serializable
    data object AssistantScreen : Screen()
}
