package com.mhss.app.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.database.entity.BookmarkEntity
import com.mhss.app.database.entity.DiaryEntryEntity
import com.mhss.app.database.entity.NoteEntity
import com.mhss.app.database.entity.NoteFolderEntity
import com.mhss.app.database.entity.TaskEntity
import com.mhss.app.database.entity.withoutIds
import com.mhss.app.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import com.mhss.app.util.CryptoManager

@Single
class BackupRepositoryImpl(
    private val context: Context,
    private val database: MyBrainDatabase,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : BackupRepository {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun exportDatabase(
        directoryUri: String,
        exportNotes: Boolean,
        exportTasks: Boolean,
        exportDiary: Boolean,
        exportBookmarks: Boolean,
        encrypted: Boolean, // To be added in a future version
        password: String // To be added in a future version
    ): Boolean {
        return withContext(ioDispatcher) {
            try {
                val fileName = "Vord_Backup_${System.currentTimeMillis()}.json"
                val pickedDir = DocumentFile.fromTreeUri(context, directoryUri.toUri())
                    ?: return@withContext false

                val destination = pickedDir.createFile("application/json", fileName)
                    ?: return@withContext false

                val notes = if (exportNotes) database.noteDao().getAllNotes() else emptyList()
                val noteFolders = if (exportNotes) database.noteDao().getAllNoteFolders().first() else emptyList()
                val tasks = if (exportTasks) database.taskDao().getAllTasks().first() else emptyList()
                val diary = if (exportDiary) database.diaryDao().getAllEntries().first() else emptyList()
                val bookmarks =
                    if (exportBookmarks) database.bookmarkDao().getAllBookmarks().first() else emptyList()

                val backupData = BackupData(
                    notes = notes,
                    noteFolders = noteFolders,
                    tasks = tasks,
                    diary = diary,
                    bookmarks = bookmarks
                )

                val jsonBytes = Json.encodeToString(
                    BackupData.serializer(),
                    backupData
                ).toByteArray(Charsets.UTF_8)

                val finalBytes = if (encrypted && password.isNotBlank()) {
                    CryptoManager.encrypt(jsonBytes, password)
                } else {
                    jsonBytes
                }

                destination.uri?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(finalBytes)
                    } ?: return@withContext false
                } ?: return@withContext false

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun importDatabase(
        fileUri: String,
        encrypted: Boolean,
        password: String
    ): Boolean {
        return withContext(ioDispatcher) {
            try {
                val json = Json {
                    ignoreUnknownKeys = true
                }

                val bytes = context.contentResolver
                    .openInputStream(fileUri.toUri())
                    ?.readBytes()
                    ?: return@withContext false

                val jsonBytes = if (encrypted && password.isNotBlank()) {
                    CryptoManager.decrypt(bytes, password)
                } else {
                    bytes
                }

                val backupData = json.decodeFromString<BackupData>(
                    jsonBytes.toString(Charsets.UTF_8)
                )

                val oldNoteFolderIdsMap = HashMap<Int, Int>()
                for ((index, folder) in backupData.noteFolders.withIndex()) {
                    oldNoteFolderIdsMap[folder.id] = index
                }

                database.withTransaction {
                    val newNoteFolderIds =
                        database.noteDao().insertNoteFolders(
                            backupData.noteFolders.withoutIds()
                        )

                    val notes = if (newNoteFolderIds.size != oldNoteFolderIdsMap.size) {
                        Log.d(
                            "BackupRepositoryImpl.importDatabase",
                            "New folder count (${newNoteFolderIds.size}) does not match old folder count (${oldNoteFolderIdsMap.size})"
                        )
                        backupData.notes.withoutIds()
                    } else {
                        backupData.notes.map { note ->
                            note.copy(
                                folderId = note.folderId?.let {
                                    newNoteFolderIds[oldNoteFolderIdsMap[it]!!].toInt()
                                },
                                id = 0
                            )
                        }
                    }
                    database.noteDao().insertNotes(notes)
                    database.taskDao().insertTasks(backupData.tasks.withoutIds())
                    database.diaryDao().insertEntries(backupData.diary.withoutIds())
                    database.bookmarkDao().insertBookmarks(backupData.bookmarks.withoutIds())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    @Serializable
    private data class BackupData(
        @SerialName("notes") val notes: List<NoteEntity> = emptyList(),
        @SerialName("noteFolders") val noteFolders: List<NoteFolderEntity> = emptyList(),
        @SerialName("tasks") val tasks: List<TaskEntity> = emptyList(),
        @SerialName("diary") val diary: List<DiaryEntryEntity> = emptyList(),
        @SerialName("bookmarks") val bookmarks: List<BookmarkEntity> = emptyList()
    )
}