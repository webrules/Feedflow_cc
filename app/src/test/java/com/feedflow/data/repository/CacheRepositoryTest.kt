package com.feedflow.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feedflow.data.local.db.FeedflowDatabase
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheRepositoryTest {

    private lateinit var database: FeedflowDatabase
    private lateinit var cacheRepository: CacheRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FeedflowDatabase::class.java
        ).allowMainThreadQueries().build()

        cacheRepository = CacheRepository(
            database.cacheDao(),
            database.aiSummaryDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndGetCachedTopics() = runTest {
        val cacheKey = "test_key"
        val topics = listOf(
            ForumThread(
                id = "1",
                title = "Title",
                content = "Content",
                author = User("u1", "User", ""),
                community = Community("c1", "Community", "", "rss"),
                timeAgo = "time",
                likeCount = 0,
                commentCount = 0
            )
        )

        cacheRepository.saveCachedTopics(cacheKey, topics)

        val retrieved = cacheRepository.getCachedTopics(cacheKey)
        assertNotNull(retrieved)
        assertEquals(1, retrieved!!.size)
        assertEquals("Title", retrieved[0].title)
    }

    @Test
    fun saveAndGetCachedThread() = runTest {
        val threadId = "1"
        val thread = ForumThread(
            id = threadId,
            title = "Title",
            content = "Content",
            author = User("u1", "User", ""),
            community = Community("c1", "Community", "", "rss"),
            timeAgo = "time",
            likeCount = 0,
            commentCount = 0
        )
        val comments = emptyList<Comment>()

        cacheRepository.saveCachedThread(threadId, thread, comments)

        val retrieved = cacheRepository.getCachedThread(threadId)
        assertNotNull(retrieved)
        assertEquals("Title", retrieved!!.first.title)
    }
}
