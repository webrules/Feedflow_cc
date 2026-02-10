package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feedflow.data.local.db.entity.CachedThreadEntity
import com.feedflow.data.local.db.entity.CachedTopicEntity

@Dao
interface CacheDao {
    // Cached Topics (Thread Lists)
    @Query("SELECT * FROM cached_topics WHERE cacheKey = :cacheKey")
    suspend fun getCachedTopics(cacheKey: String): CachedTopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedTopics(cachedTopic: CachedTopicEntity)

    @Query("DELETE FROM cached_topics WHERE cacheKey = :cacheKey")
    suspend fun deleteCachedTopics(cacheKey: String)

    @Query("DELETE FROM cached_topics WHERE timestamp < :olderThan")
    suspend fun deleteOldTopics(olderThan: Long)

    // Cached Threads (Thread Details)
    @Query("SELECT * FROM cached_threads WHERE threadId = :threadId")
    suspend fun getCachedThread(threadId: String): CachedThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedThread(cachedThread: CachedThreadEntity)

    @Query("DELETE FROM cached_threads WHERE threadId = :threadId")
    suspend fun deleteCachedThread(threadId: String)

    @Query("DELETE FROM cached_threads WHERE timestamp < :olderThan")
    suspend fun deleteOldThreads(olderThan: Long)

    // Clear all cache
    @Query("DELETE FROM cached_topics")
    suspend fun deleteAllTopics()

    @Query("DELETE FROM cached_threads")
    suspend fun deleteAllThreads()
}
