package com.mhss.app.mybrain.presentation.auth

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mhss.app.database.dao.BookmarkDao
import com.mhss.app.database.dao.DiaryDao
import com.mhss.app.database.dao.NoteDao
import com.mhss.app.database.dao.TaskDao
import com.mhss.app.database.entity.BookmarkEntity
import com.mhss.app.database.entity.DiaryEntryEntity
import com.mhss.app.database.entity.NoteEntity
import com.mhss.app.database.entity.NoteFolderEntity
import com.mhss.app.database.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import org.koin.android.annotation.KoinViewModel
import kotlinx.coroutines.flow.asStateFlow
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.mybrain.InternetDetector
import android.app.Application
import com.benasher44.uuid.Uuid


@KoinViewModel
class AuthViewModel(
    private val app: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: MyBrainDatabase,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val diaryDao: DiaryDao,
    private val bookmarkDao: BookmarkDao
) : ViewModel() {

    private var syncPaused: Boolean = false

    private val pendingSyncQueue =
        ArrayDeque<suspend () -> Unit>()

    private val queueFileName = "pending_sync_queue.flag"

    private fun persistQueueFlag() {
        val file = java.io.File(app.filesDir, queueFileName)
        file.writeText("pending")
    }
    private fun clearQueueFlag() {
        val file = java.io.File(app.filesDir, queueFileName)
        if (file.exists()) {
            file.delete()
        }
    }
    private fun hasPersistedQueue(): Boolean {
        val file = java.io.File(app.filesDir, queueFileName)
        return file.exists()
    }
    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private var wasPreviouslyLoggedIn = false

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val currentUser = firebaseAuth.currentUser
        val isNowLoggedIn = currentUser != null

        _user.value = currentUser

        // Reagera endast vid tillståndsförändring
        if (isNowLoggedIn != wasPreviouslyLoggedIn) {
            if (isNowLoggedIn) {
                _authMessage.value = "Du är nu inloggad – din data synkas till enheten"
                viewModelScope.launch {
                    downloadAllFromCloud()
                }
            } else {
                _authMessage.value = "Du är nu utloggad"
            }
        }

        wasPreviouslyLoggedIn = isNowLoggedIn
    }

    init {
        // Sätt korrekt initialt tillstånd innan vi lyssnar
        wasPreviouslyLoggedIn = auth.currentUser != null
        _user.value = auth.currentUser

        auth.addAuthStateListener(authListener)
        if (hasPersistedQueue()) {
            syncPaused = true
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    // ────────────────────────────────────────────────
    //    Inloggning / Skapa konto
    // ────────────────────────────────────────────────
    fun signIn(email: String, password: String, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _user.value = result.user
                _authMessage.value = "Du är nu inloggad – din data synkas till enheten"
                // Nedladdning hanteras av authListener
            }
            .addOnFailureListener {
                // Försök skapa nytt konto
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        _user.value = result.user
                        _authMessage.value = "Konto skapat och du är nu inloggad"
                    }
                    .addOnFailureListener { exception ->
                        _authMessage.value = "Inloggning misslyckades – fel användarnamn eller lösenord"
                        onError("Authentication failed: ${exception.localizedMessage}")
                    }
            }
    }

    fun signOut() {
        auth.signOut()
        // Resten (null:a user + meddelande) hanteras av authListener
    }

    // ────────────────────────────────────────────────
//    Manuell synkronisering uppåt (upload)
// ────────────────────────────────────────────────
    fun resyncAllFromCloud() {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    downloadAllFromCloud()
                }
            } finally {
                _syncInProgress.value = false
            }
        }
    }

    fun syncNow(onDone: () -> Unit = {}) {
        val user = auth.currentUser ?: run {
            _syncResult.value = false
            onDone()
            return
        }

        _syncInProgress.value = true
        android.util.Log.d("SYNC", "syncNow() starting for uid=${user.uid}")

        viewModelScope.launch {

            if (!InternetDetector.hasInternet(app)) {
                _syncResult.value = false
                _syncInProgress.value = false
                onDone()
                return@launch
            }
            syncPaused = false
            resumePendingSyncIfAny()
            try {
                withContext(Dispatchers.IO) {
                    val notes = noteDao.getAllNotes()
                    val noteFolders = noteDao.getAllNoteFolders().first()
                    val tasks = taskDao.getAllTasks().first()
                    val diary = diaryDao.getAllEntries().first()
                    val bookmarks = bookmarkDao.getAllBookmarks().first()

                    val userRef = userRef(user.uid)
                    val batch = firestore.batch()

                    notes.filter { it.id != 0 }.forEach { n ->
                        batch.set(
                            userRef.collection("notes").document(n.id.toString()),
                            n.toNoteMap()
                        )
                    }
                    noteFolders.filter { it.id != 0 }.forEach { f ->
                        batch.set(
                            userRef.collection("folders").document(f.id.toString()),
                            f.toFolderMap()
                        )
                    }
                    tasks.filter { it.id != 0 }.forEach { t ->
                        batch.set(
                            userRef.collection("tasks").document(t.id.toString()),
                            t.toTaskMap()
                        )
                    }
                    diary.filter { it.id != 0 }.forEach { d ->
                        batch.set(
                            userRef.collection("diary").document(d.id.toString()),
                            d.toDiaryMap()
                        )
                    }
                    bookmarks.filter { it.id != 0 }.forEach { b ->
                        batch.set(
                            userRef.collection("bookmarks").document(b.id.toString()),
                            b.toBookmarkMap()
                        )
                    }

                    val meta = hashMapOf(
                        "device" to Build.MODEL,
                        "lastSync" to FieldValue.serverTimestamp()
                    )
                    batch.set(userRef.collection("meta").document("sync"), meta)

                    batch.commit().await()
                }

                _syncResult.value = true
                android.util.Log.d("SYNC", "syncNow() completed OK")
            } catch (e: Exception) {
                _syncResult.value = false
                android.util.Log.e("SYNC", "syncNow() failed: ${e.message}")
            } finally {
                _syncInProgress.value = false
                onDone()
            }
        }
    }


    // ────────────────────────────────────────────────
    //    Nedladdning från molnet (anropas automatiskt vid ny inloggning)
    // ────────────────────────────────────────────────
    private suspend fun resumePendingSyncIfAny() {
        if (syncPaused) return
        if (pendingSyncQueue.isEmpty()) return

        while (pendingSyncQueue.isNotEmpty()) {
            val block = pendingSyncQueue.first()
            try {
                block()
                pendingSyncQueue.removeFirst()
            } catch (e: Exception) {
                syncPaused = true
                persistQueueFlag()
                return
            }
        }

        clearQueueFlag()
    }
    private suspend fun downloadAllFromCloud() {
        val user = auth.currentUser ?: run {
            android.util.Log.w("SYNC", "downloadAllFromCloud: no currentUser, aborting")
            return
        }

        android.util.Log.d("SYNC", "downloadAllFromCloud: ENTER for uid=${user.uid}")
        _syncInProgress.value = true

        try {
            withContext(Dispatchers.IO) {
                val base = firestore.collection("workspace").document("shared")

                // Folders
                runCatching {
                    val snap = base.collection("folders").get().await()
                    val folders = snap.documents.mapNotNull { d ->
                        val data = d.data ?: return@mapNotNull null
                        NoteFolderEntity(
                            id = (data["id"] as? Number)?.toInt() ?: 0,
                            name = data["name"] as? String ?: "",
                            parentId = (data["parentId"] as? Number)?.toInt()
                        )
                    }
                    if (folders.isNotEmpty()) noteDao.insertNoteFolders(folders)
                }

                // Notes
                runCatching {
                    val snap = base.collection("notes").get().await()
                    val notes = snap.documents.mapNotNull { d ->
                        val data = d.data ?: return@mapNotNull null
                        NoteEntity(
                            title = data["title"] as? String ?: "",
                            content = data["content"] as? String ?: "",
                            createdDate = (data["createdDate"] as? Number)?.toLong() ?: 0L,
                            updatedDate = (data["updatedDate"] as? Number)?.toLong() ?: 0L,
                            pinned = data["pinned"] as? Boolean ?: false,
                            folderId = (data["folderId"] as? Number)?.toInt(),
                            id = (data["id"] as? Number)?.toInt() ?: 0
                        )
                    }
                    if (notes.isNotEmpty()) noteDao.insertNotes(notes)
                }

                // Diary
                runCatching {
                    val snap = base.collection("diary").get().await()
                    val entries = snap.documents.mapNotNull { d ->
                        val data = d.data ?: return@mapNotNull null
                        val mood = when (val mv = data["mood"]) {
                            is Number -> com.mhss.app.domain.model.Mood.values().getOrNull(mv.toInt())
                            is String -> runCatching { com.mhss.app.domain.model.Mood.valueOf(mv) }.getOrNull()
                            else -> null
                        } ?: com.mhss.app.domain.model.Mood.values().first()
                        DiaryEntryEntity(
                            title = data["title"] as? String ?: "",
                            content = data["content"] as? String ?: "",
                            createdDate = (data["createdDate"] as? Number)?.toLong() ?: 0L,
                            updatedDate = (data["updatedDate"] as? Number)?.toLong() ?: 0L,
                            mood = mood,
                            id = (data["id"] as? Number)?.toInt() ?: 0
                        )
                    }
                    if (entries.isNotEmpty()) diaryDao.insertEntries(entries)
                }
// Tasks
                runCatching {
                    val snap = base.collection("tasks").get().await()
                    val tasks = snap.documents.mapNotNull { d ->
                        val data = d.data ?: return@mapNotNull null
                        TaskEntity(
                            id = (data["id"] as? Number)?.toInt() ?: 0,
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            isCompleted = data["isCompleted"] as? Boolean ?: false,
                            priority = (data["priority"] as? Number)?.toInt() ?: 0,
                            createdDate = (data["createdDate"] as? Number)?.toLong() ?: 0L,
                            updatedDate = (data["updatedDate"] as? Number)?.toLong() ?: 0L,
                            subTasks = (data["subTasks"] as? List<*>)?.mapNotNull { raw ->
                                val map = raw as? Map<*, *> ?: return@mapNotNull null
                                val idValue = map["id"] ?: return@mapNotNull null
                                val uuid = when (idValue) {
                                    is String -> try { com.benasher44.uuid.Uuid.fromString(idValue) } catch (e: Exception) { return@mapNotNull null }
                                    else -> return@mapNotNull null
                                }
                                com.mhss.app.domain.model.SubTask(
                                    id = uuid,
                                    title = map["title"] as? String ?: "",
                                    isCompleted = map["isCompleted"] as? Boolean ?: false
                                )
                            } ?: emptyList(),
                            dueDate = (data["dueDate"] as? Number)?.toLong() ?: 0L,
                            recurring = data["recurring"] as? Boolean ?: false,
                            frequency = (data["frequency"] as? Number)?.toInt() ?: 0,
                            frequencyAmount = (data["frequencyAmount"] as? Number)?.toInt() ?: 0
                        )

                    }
                    if (tasks.isNotEmpty()) taskDao.insertTasks(tasks)
                }

                // Bookmarks
                runCatching {
                    val snap = base.collection("bookmarks").get().await()
                    val bookmarks = snap.documents.mapNotNull { d ->
                        val data = d.data ?: return@mapNotNull null
                        BookmarkEntity(
                            url = data["url"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            createdDate = (data["createdDate"] as? Number)?.toLong() ?: 0L,
                            updatedDate = (data["updatedDate"] as? Number)?.toLong() ?: 0L,
                            id = (data["id"] as? Number)?.toInt() ?: 0
                        )
                    }
                    if (bookmarks.isNotEmpty()) bookmarkDao.insertBookmarks(bookmarks)
                }

                touchMeta()
            }

            _syncResult.value = true
            android.util.Log.d("SYNC", "downloadAllFromCloud: EXIT success")
        } catch (e: Exception) {
            _syncResult.value = false
            android.util.Log.e("SYNC", "downloadAllFromCloud: ERROR -> ${e.message}")
        } finally {
            _syncInProgress.value = false
        }
    }

    // ────────────────────────────────────────────────
    //    Automatisk enskild synkronisering vid ändring
    // ────────────────────────────────────────────────
    fun syncNote(note: NoteEntity) {
        val user = auth.currentUser ?: return
        if (note.id == 0) return

        viewModelScope.launch(Dispatchers.IO) {
            enqueueOrRun {
                userRef(user.uid)
                    .collection("notes")
                    .document(note.id.toString())
                    .set(note.toNoteMap())
                    .await()

                touchMeta()
            }
        }
    }
    fun syncBookmark(bookmark: BookmarkEntity) {
        val user = auth.currentUser ?: return
        if (bookmark.id == 0) return

        viewModelScope.launch(Dispatchers.IO) {
            enqueueOrRun {
                userRef(user.uid)
                    .collection("bookmarks")
                    .document(bookmark.id.toString())
                    .set(bookmark.toBookmarkMap())
                    .await()

                touchMeta()
            }
        }
    }

    // ────────────────────────────────────────────────
    //    Hjälpfunktioner
    // ────────────────────────────────────────────────
    private suspend fun enqueueOrRun(block: suspend () -> Unit) {
        if (syncPaused) {
            pendingSyncQueue.addLast(block)
            persistQueueFlag()
            return
        }

        try {
            block()
        } catch (e: Exception) {
            syncPaused = true
            pendingSyncQueue.addLast(block)
            persistQueueFlag()
        }
    }

    private fun userRef(uid: String): DocumentReference =
        firestore.collection("workspace").document("shared")

    private suspend fun touchMeta() {
        val uid = auth.currentUser?.uid ?: return
        val metaDoc = userRef(uid).collection("meta").document("sync")
        val meta = hashMapOf(
            "device" to Build.MODEL,
            "lastSync" to FieldValue.serverTimestamp()
        )
        metaDoc.set(meta).await()
    }

    private fun NoteEntity.toNoteMap(): HashMap<String, Any?> = hashMapOf(
        "title" to title,
        "content" to content,
        "createdDate" to createdDate,
        "updatedDate" to updatedDate,
        "pinned" to pinned,
        "folderId" to folderId,
        "id" to id
    )

    private fun NoteFolderEntity.toFolderMap(): HashMap<String, Any?> = hashMapOf(
        "id" to id,
        "name" to name,
        "parentId" to parentId
    )

    private fun TaskEntity.toTaskMap(): HashMap<String, Any?> {
        val subTasksList = subTasks.map {
            hashMapOf(
                "title" to it.title,
                "isCompleted" to it.isCompleted,
                "id" to it.id.toString()
            )
        }
        return hashMapOf(
            "title" to title,
            "description" to description,
            "isCompleted" to isCompleted,
            "priority" to priority,
            "createdDate" to createdDate,
            "updatedDate" to updatedDate,
            "subTasks" to subTasksList,
            "dueDate" to dueDate,
            "recurring" to recurring,
            "frequency" to frequency,
            "frequencyAmount" to frequencyAmount,
            "id" to id
        )
    }

    private fun DiaryEntryEntity.toDiaryMap(): HashMap<String, Any?> = hashMapOf(
        "title" to title,
        "content" to content,
        "createdDate" to createdDate,
        "updatedDate" to updatedDate,
        "mood" to mood.ordinal,
        "id" to id
    )

    private fun BookmarkEntity.toBookmarkMap(): HashMap<String, Any?> = hashMapOf(
        "url" to url,
        "title" to title,
        "description" to description,
        "createdDate" to createdDate,
        "updatedDate" to updatedDate,
        "id" to id
    )

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { res -> cont.resume(res) {} }
        addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }


    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    private val _syncResult = MutableStateFlow<Boolean?>(null)
    val syncResult: StateFlow<Boolean?> = _syncResult.asStateFlow()
}