package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.BookmarkDao
import com.feedflow.data.local.db.entity.BookmarkEntity
import com.feedflow.data.local.db.entity.UrlBookmarkEntity
import com.feedflow.data.model.ForumThread
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Thread Bookmarks
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun getAllBookmarksOnce(): List<Pair<ForumThread, String>> {
        return bookmarkDao.getAllBookmarksOnce().mapNotNull { entity ->
            try {
                val thread = json.decodeFromString<ForumThread>(entity.data)
                thread to entity.serviceId
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun isBookmarked(threadId: String, serviceId: String): Boolean {
        return bookmarkDao.isBookmarked(threadId, serviceId)
    }

    fun isBookmarkedFlow(threadId: String, serviceId: String): Flow<Boolean> {
        return bookmarkDao.isBookmarkedFlow(threadId, serviceId)
    }

    suspend fun toggleBookmark(thread: ForumThread, serviceId: String) {
        if (bookmarkDao.isBookmarked(thread.id, serviceId)) {
            bookmarkDao.deleteBookmark(thread.id, serviceId)
        } else {
            val entity = BookmarkEntity(
                threadId = thread.id,
                serviceId = serviceId,
                data = json.encodeToString(thread),
                timestamp = System.currentTimeMillis()
            )
            bookmarkDao.insertBookmark(entity)
        }
    }

    suspend fun addBookmark(thread: ForumThread, serviceId: String) {
        val entity = BookmarkEntity(
            threadId = thread.id,
            serviceId = serviceId,
            data = json.encodeToString(thread),
            timestamp = System.currentTimeMillis()
        )
        bookmarkDao.insertBookmark(entity)
    }

    suspend fun removeBookmark(threadId: String, serviceId: String) {
        bookmarkDao.deleteBookmark(threadId, serviceId)
    }

    // URL Bookmarks
    fun getAllUrlBookmarks(): Flow<List<UrlBookmarkEntity>> = bookmarkDao.getAllUrlBookmarks()

    suspend fun getAllUrlBookmarksOnce(): List<UrlBookmarkEntity> {
        return bookmarkDao.getAllUrlBookmarksOnce()
    }

    suspend fun isUrlBookmarked(url: String): Boolean {
        return bookmarkDao.isUrlBookmarked(url)
    }

    fun isUrlBookmarkedFlow(url: String): Flow<Boolean> {
        return bookmarkDao.isUrlBookmarkedFlow(url)
    }

    suspend fun toggleUrlBookmark(url: String, title: String) {
        if (bookmarkDao.isUrlBookmarked(url)) {
            bookmarkDao.deleteUrlBookmark(url)
        } else {
            val entity = UrlBookmarkEntity(
                url = url,
                title = title,
                timestamp = System.currentTimeMillis()
            )
            bookmarkDao.insertUrlBookmark(entity)
        }
    }

    suspend fun addUrlBookmark(url: String, title: String) {
        val entity = UrlBookmarkEntity(
            url = url,
            title = title,
            timestamp = System.currentTimeMillis()
        )
        bookmarkDao.insertUrlBookmark(entity)
    }

    suspend fun removeUrlBookmark(url: String) {
        bookmarkDao.deleteUrlBookmark(url)
    }
}
