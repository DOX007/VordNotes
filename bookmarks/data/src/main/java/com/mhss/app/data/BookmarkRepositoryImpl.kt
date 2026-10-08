package com.mhss.app.data

import com.mhss.app.database.dao.BookmarkDao
import com.mhss.app.database.entity.toBookmark
import com.mhss.app.database.entity.toBookmarkEntity
import com.mhss.app.domain.model.Bookmark
import com.mhss.app.domain.repository.BookmarkRepository
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
import com.mhss.app.database.remote.toBookmarkDoc

@Single
class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BookmarkRepository {

    override fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks()
            .flowOn(ioDispatcher)
            .map { bookmarks ->
                bookmarks.map {
                    it.toBookmark()
                }
            }
    }

    override suspend fun getBookmark(id: Int): Bookmark {
        return withContext(ioDispatcher) {
            bookmarkDao.getBookmark(id).toBookmark()
        }
    }

    override suspend fun searchBookmarks(query: String): List<Bookmark> {
        return withContext(ioDispatcher) {
            bookmarkDao.getBookmark(query).map { it.toBookmark() }
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark): Long {
        return withContext(ioDispatcher) {
            val newId = bookmarkDao.insertBookmark(bookmark.toBookmarkEntity())
            val u = auth.currentUser
            if (u != null && newId != 0L) {
                val entity = bookmark.toBookmarkEntity().copy(id = newId.toInt())
                firestore.collection("workspace").document("shared")
                    .collection("bookmarks").document(entity.id.toString())
                    .set(toBookmarkDoc(entity))
                    .await()
            }
            newId
        }
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        withContext(ioDispatcher) {
            val entity = bookmark.toBookmarkEntity()
            // 1) Lokalt
            bookmarkDao.deleteBookmark(entity)

            // 2) Firestore
            val u = auth.currentUser
            if (u != null && entity.id != 0) {
                firestore.collection("workspace").document("shared")
                    .collection("bookmarks").document(entity.id.toString())
                    .delete()
                    .await()
            }
        }
    }

    override suspend fun updateBookmark(bookmark: Bookmark) {
        withContext(ioDispatcher) {
            bookmarkDao.updateBookmark(bookmark.toBookmarkEntity())
            val u = auth.currentUser
            if (u != null && bookmark.id != 0) {
                val entity = bookmark.toBookmarkEntity()
                firestore.collection("workspace").document("shared")
                    .collection("bookmarks").document(entity.id.toString())
                    .set(toBookmarkDoc(entity))
                    .await()
            }
        }
    }
}