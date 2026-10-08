package com.mhss.app.data

import com.mhss.app.database.dao.NoteDao
import com.mhss.app.database.entity.toNote
import com.mhss.app.database.entity.toNoteEntity
import com.mhss.app.database.entity.toNoteFolder
import com.mhss.app.database.entity.toNoteFolderEntity
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.NoteFolder
import com.mhss.app.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mhss.app.database.remote.await
import com.mhss.app.database.remote.toNoteDoc
import com.mhss.app.database.remote.toFolderDoc

@Single
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : NoteRepository {

    override fun getAllFolderlessNotes(): Flow<List<Note>> {
        return noteDao.getAllFolderlessNotes()
            .flowOn(ioDispatcher)
            .map { notes ->
                notes.map {
                    it.toNote()
                }
            }
    }

    override suspend fun getNote(id: Int): Note {
        return withContext(ioDispatcher) {
            noteDao.getNote(id).toNote()
        }
    }

    override suspend fun searchNotes(query: String): List<Note> {
        return withContext(ioDispatcher) {
            noteDao.getNotesByTitle(query).map {
                it.toNote()
            }
        }
    }

    override fun getNotesByFolder(folderId: Int): Flow<List<Note>> {
        return noteDao.getNotesByFolder(folderId)
            .flowOn(ioDispatcher)
            .map { notes ->
                notes.map { it.toNote() }
            }
    }

    override suspend fun addNote(note: Note): Long {
        return withContext(ioDispatcher) {
            val newId = noteDao.insertNote(note.toNoteEntity())
            val u = auth.currentUser
            if (u != null && newId != 0L) {
                val entity = note.toNoteEntity().copy(id = newId.toInt())
                firestore.collection("workspace").document("shared")
                    .collection("notes").document(entity.id.toString())
                    .set(toNoteDoc(entity))
                    .await()
            }
            newId
        }
    }

    override suspend fun updateNote(note: Note) {
        withContext(ioDispatcher) {
            noteDao.updateNote(note.toNoteEntity())
            val u = auth.currentUser
            if (u != null && note.id != 0) {
                val entity = note.toNoteEntity()
                firestore.collection("workspace").document("shared")
                    .collection("notes").document(entity.id.toString())
                    .set(toNoteDoc(entity))
                    .await()
            }
        }
    }

    override suspend fun deleteNote(note: Note) {
        withContext(ioDispatcher) {
            val entity = note.toNoteEntity()
            // 1) Lokalt
            noteDao.deleteNote(entity)

            // 2) Firestore
            val u = auth.currentUser
            if (u != null && entity.id != 0) {
                firestore.collection("workspace").document("shared")
                    .collection("notes").document(entity.id.toString())
                    .delete()
                    .await()
            }
        }
    }

    override suspend fun insertNoteFolder(folder: NoteFolder) {
        withContext(ioDispatcher) {
            // 1) Insert i Room och hämta genererat ID
            val newId = noteDao.insertNoteFolder(folder.toNoteFolderEntity())

            // 2) Skriv till molnet ENDAST med rätt id
            val u = auth.currentUser
            if (u != null && newId > 0L) {
                val entity = folder.toNoteFolderEntity().copy(id = newId.toInt())
                firestore.collection("workspace").document("shared")
                    .collection("folders").document(entity.id.toString())
                    .set(toFolderDoc(entity))
                    .await()
            }
        }
    }

    override suspend fun updateNoteFolder(folder: NoteFolder) {
        withContext(ioDispatcher) {
            noteDao.updateNoteFolder(folder.toNoteFolderEntity())
            val u = auth.currentUser
            if (u != null && folder.id != 0) {
                val entity = folder.toNoteFolderEntity()
                firestore.collection("workspace").document("shared")
                    .collection("folders").document(entity.id.toString())
                    .set(toFolderDoc(entity))
                    .await()
            }
        }
    }

    override suspend fun deleteNoteFolder(folder: NoteFolder) {
        withContext(ioDispatcher) {
            val entity = folder.toNoteFolderEntity()
            // 1) Lokalt
            noteDao.deleteNoteFolder(entity)

            // 2) Firestore (spegla insert/update-konventionen för "folders")
            val u = auth.currentUser
            if (u != null && entity.id != 0) {
                firestore.collection("workspace").document("shared")
                    .collection("folders").document(entity.id.toString())
                    .delete()
                    .await()
            }
        }
    }

    override fun getAllNoteFolders(): Flow<List<NoteFolder>> {
        return noteDao.getAllNoteFolders()
            .flowOn(ioDispatcher)
            .map { folders ->
                folders.map { it.toNoteFolder() }
            }
    }

    override suspend fun getNoteFolder(folderId: Int): NoteFolder? {
        return withContext(ioDispatcher) {
            noteDao.getNoteFolder(folderId)?.toNoteFolder()
        }
    }
}
