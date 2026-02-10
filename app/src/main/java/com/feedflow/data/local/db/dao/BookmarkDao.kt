package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feedflow.data.local.db.entity.BookmarkEntity
import com.feedflow.data.local.db.entity.UrlBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    // Thread Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE threadId = :threadId AND serviceId = :serviceId")
    suspend fun getBookmark(threadId: String, serviceId: String): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE threadId = :threadId AND serviceId = :serviceId)")
    suspend fun isBookmarked(threadId: String, serviceId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE threadId = :threadId AND serviceId = :serviceId)")
    fun isBookmarkedFlow(threadId: String, serviceId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE threadId = :threadId AND serviceId = :serviceId")
    suspend fun deleteBookmark(threadId: String, serviceId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    // URL Bookmarks
    @Query("SELECT * FROM url_bookmarks ORDER BY timestamp DESC")
    fun getAllUrlBookmarks(): Flow<List<UrlBookmarkEntity>>

    @Query("SELECT * FROM url_bookmarks ORDER BY timestamp DESC")
    suspend fun getAllUrlBookmarksOnce(): List<UrlBookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM url_bookmarks WHERE url = :url)")
    suspend fun isUrlBookmarked(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM url_bookmarks WHERE url = :url)")
    fun isUrlBookmarkedFlow(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrlBookmark(bookmark: UrlBookmarkEntity)

    @Query("DELETE FROM url_bookmarks WHERE url = :url")
    suspend fun deleteUrlBookmark(url: String)

    @Query("DELETE FROM url_bookmarks")
    suspend fun deleteAllUrlBookmarks()
}
