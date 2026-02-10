package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.AiSummaryDao
import com.feedflow.data.local.db.dao.CacheDao
import com.feedflow.data.local.db.entity.AiSummaryEntity
import com.feedflow.data.local.db.entity.CachedThreadEntity
import com.feedflow.data.local.db.entity.CachedTopicEntity
import com.feedflow.data.model.Comment
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailCache
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
    private val cacheDao: CacheDao,
    private val aiSummaryDao: AiSummaryDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cached Topics (Thread Lists)
    suspend fun getCachedTopics(cacheKey: String): List<ForumThread>? {
        val entity = cacheDao.getCachedTopics(cacheKey) ?: return null
        return try {
            json.decodeFromString<List<ForumThread>>(entity.data)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCachedTopics(cacheKey: String, topics: List<ForumThread>) {
        val entity = CachedTopicEntity(
            cacheKey = cacheKey,
            data = json.encodeToString(topics),
            timestamp = System.currentTimeMillis()
        )
        cacheDao.saveCachedTopics(entity)
    }

    suspend fun deleteCachedTopics(cacheKey: String) {
        cacheDao.deleteCachedTopics(cacheKey)
    }

    // Cached Threads (Thread Details)
    suspend fun getCachedThread(threadId: String): Pair<ForumThread, List<Comment>>? {
        val entity = cacheDao.getCachedThread(threadId) ?: return null
        return try {
            val cache = json.decodeFromString<ThreadDetailCache>(entity.data)
            cache.thread to cache.comments
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasCache(threadId: String): Boolean {
        return cacheDao.getCachedThread(threadId) != null
    }

    suspend fun saveCachedThread(threadId: String, thread: ForumThread, comments: List<Comment>) {
        val cache = ThreadDetailCache(thread = thread, comments = comments)
        val entity = CachedThreadEntity(
            threadId = threadId,
            data = json.encodeToString(cache),
            timestamp = System.currentTimeMillis()
        )
        cacheDao.saveCachedThread(entity)
    }

    suspend fun deleteCachedThread(threadId: String) {
        cacheDao.deleteCachedThread(threadId)
    }

    // AI Summaries
    suspend fun getSummary(threadId: String): String? {
        return aiSummaryDao.getSummary(threadId)?.summary
    }

    suspend fun getSummaryIfFresh(threadId: String, maxAgeSeconds: Long): String? {
        val entity = aiSummaryDao.getSummary(threadId) ?: return null
        return if (entity.isFresh(maxAgeSeconds)) entity.summary else null
    }

    suspend fun saveSummary(threadId: String, summary: String) {
        val entity = AiSummaryEntity(
            threadId = threadId,
            summary = summary,
            createdAt = System.currentTimeMillis()
        )
        aiSummaryDao.saveSummary(entity)
    }

    suspend fun deleteSummary(threadId: String) {
        aiSummaryDao.deleteSummary(threadId)
    }

    // Cleanup
    suspend fun cleanupOldCache(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) { // 7 days
        val olderThan = System.currentTimeMillis() - maxAgeMs
        cacheDao.deleteOldTopics(olderThan)
        cacheDao.deleteOldThreads(olderThan)
        aiSummaryDao.deleteOldSummaries(olderThan)
    }

    suspend fun clearAllCache() {
        cacheDao.deleteAllTopics()
        cacheDao.deleteAllThreads()
        aiSummaryDao.deleteAll()
    }
}
