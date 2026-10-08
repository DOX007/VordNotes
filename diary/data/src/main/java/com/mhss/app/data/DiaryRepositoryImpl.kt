package com.mhss.app.data

import com.mhss.app.database.dao.DiaryDao
import com.mhss.app.database.entity.*
import com.mhss.app.domain.repository.DiaryRepository
import com.mhss.app.domain.model.DiaryEntry
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
import com.mhss.app.database.remote.toDiaryDoc
@Single
class DiaryRepositoryImpl(
    private val diaryDao: DiaryDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DiaryRepository {

    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        return diaryDao.getAllEntries()
            .flowOn(ioDispatcher)
            .map { entries ->
                entries.map { it.toDiaryEntry() }
            }
    }

    override suspend fun getEntry(id: Int): DiaryEntry {
        return withContext(ioDispatcher) {
            diaryDao.getEntry(id).toDiaryEntry()
        }
    }

    override suspend fun searchEntries(title: String): List<DiaryEntry> {
        return withContext(ioDispatcher) {
            diaryDao.getEntriesByTitle(title).map { it.toDiaryEntry() }
        }
    }

    override suspend fun addEntry(diary: DiaryEntry): Long {
        return withContext(ioDispatcher) {
            val newId = diaryDao.insertEntry(diary.toDiaryEntryEntity())
            val u = auth.currentUser
            if (u != null && newId != 0L) {
                val entity = diary.toDiaryEntryEntity().copy(id = newId.toInt())
                firestore.collection("workspace").document("shared")
                    .collection("diary").document(entity.id.toString())
                    .set(toDiaryDoc(entity))
                    .await()
            }
            newId
        }
    }

    override suspend fun updateEntry(diary: DiaryEntry) {
        withContext(ioDispatcher) {
            diaryDao.updateEntry(diary.toDiaryEntryEntity())
            val u = auth.currentUser
            if (u != null && diary.id != 0) {
                val entity = diary.toDiaryEntryEntity()
                firestore.collection("workspace").document("shared")
                    .collection("diary").document(entity.id.toString())
                    .set(toDiaryDoc(entity))
                    .await()
            }
        }
    }

    override suspend fun deleteEntry(diary: DiaryEntry) {
        withContext(ioDispatcher) {
            val entity = diary.toDiaryEntryEntity()
            // 1) Lokalt
            diaryDao.deleteEntry(entity)

            // 2) Firestore
            val u = auth.currentUser
            if (u != null && entity.id != 0) {
                firestore.collection("workspace").document("shared")
                    .collection("diary").document(entity.id.toString())
                    .delete()
                    .await()
            }
        }
    }
}