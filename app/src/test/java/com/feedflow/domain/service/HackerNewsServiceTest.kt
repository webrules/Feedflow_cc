package com.feedflow.domain.service

import com.feedflow.data.model.Community
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.data.remote.dto.HNItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HackerNewsServiceTest {

    private lateinit var mockApi: HackerNewsApi
    private lateinit var service: HackerNewsService

    @Before
    fun setup() {
        mockApi = mock()
        service = HackerNewsService(mockApi)
    }

    @Test
    fun fetchCategories_returnsPredefinedCategories() = runBlocking {
        val categories = service.fetchCategories()

        assertEquals(6, categories.size)
        assertEquals("topstories", categories[0].id)
        assertEquals("Top Stories", categories[0].name)
        assertEquals("hackernews", categories[0].category)
    }

    @Test
    fun fetchCategoryThreads_topStories_returnsThreads() = runBlocking {
        val storyIds = listOf(1, 2, 3)
        val item = HNItem(
            id = 1,
            type = "story",
            by = "testuser",
            time = 1705315800,
            text = "Test content",
            url = "https://example.com",
            title = "Test Story",
            score = 100,
            descendants = 10,
            kids = listOf(101, 102)
        )

        whenever(mockApi.getTopStories()).thenReturn(storyIds)
        whenever(mockApi.getItem(1)).thenReturn(item)
        whenever(mockApi.getItem(2)).thenReturn(null)
        whenever(mockApi.getItem(3)).thenReturn(item.copy(id = 3, title = "Story 3"))

        val communities = listOf(Community("topstories", "Top Stories", "", "hackernews"))
        val threads = service.fetchCategoryThreads("topstories", communities, 1)

        assertTrue(threads.isNotEmpty())
        assertEquals("Test Story", threads[0].title)
        assertEquals("testuser", threads[0].author.username)
        assertEquals(100, threads[0].likeCount)
    }

    @Test
    fun fetchCategoryThreads_pagination_returnsCorrectPage() = runBlocking {
        val storyIds = (1..50).toList()
        val item = HNItem(
            id = 21,
            type = "story",
            by = "user",
            time = 1705315800,
            text = null,
            url = "https://example.com",
            title = "Page 2 Story",
            score = 50,
            descendants = 5,
            kids = null
        )

        whenever(mockApi.getTopStories()).thenReturn(storyIds)
        whenever(mockApi.getItem(21)).thenReturn(item)
        whenever(mockApi.getItem(22)).thenReturn(item.copy(id = 22))

        val communities = listOf(Community("topstories", "Top Stories", "", "hackernews"))
        val threads = service.fetchCategoryThreads("topstories", communities, 2)

        assertTrue(threads.size <= 20)
    }

    @Test
    fun fetchCategoryThreads_emptyStoryIds_returnsEmptyList() = runBlocking {
        whenever(mockApi.getTopStories()).thenReturn(emptyList())

        val communities = listOf(Community("topstories", "Top Stories", "", "hackernews"))
        val threads = service.fetchCategoryThreads("topstories", communities, 1)

        assertTrue(threads.isEmpty())
    }

    @Test
    fun fetchThreadDetail_returnsThreadWithComments() = runBlocking {
        val item = HNItem(
            id = 1,
            type = "story",
            by = "testuser",
            time = 1705315800,
            text = "Story text",
            url = null,
            title = "Test Story",
            score = 100,
            descendants = 2,
            kids = listOf(101, 102)
        )

        val comment = HNItem(
            id = 101,
            type = "comment",
            by = "commenter",
            time = 1705316000,
            text = "Great story!",
            url = null,
            title = null,
            score = null,
            descendants = null,
            kids = null
        )

        whenever(mockApi.getItem(1)).thenReturn(item)
        whenever(mockApi.getItem(101)).thenReturn(comment)
        whenever(mockApi.getItem(102)).thenReturn(null)

        val result = service.fetchThreadDetail("1")

        assertEquals("Test Story", result.thread.title)
        assertEquals(1, result.comments.size)
        assertEquals("Great story!", result.comments[0].content)
    }

    @Test
    fun fetchThreadDetail_storyNotFound_throwsException() = runBlocking {
        whenever(mockApi.getItem(999)).thenReturn(null)

        var exceptionThrown = false
        try {
            service.fetchThreadDetail("999")
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun postComment_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.postComment("1", "cat", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun createThread_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.createThread("cat", "title", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun getWebURL_returnsCorrectUrl() {
        val thread = com.feedflow.data.model.ForumThread(
            id = "12345",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("topstories", "Top", "", "hackernews"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://news.ycombinator.com/item?id=12345", url)
    }

    @Test
    fun supportsPosting_returnsFalse() {
        assertFalse(service.supportsPosting())
    }

    @Test
    fun requiresLogin_returnsFalse() {
        assertFalse(service.requiresLogin())
    }

    @Test
    fun name_isCorrect() {
        assertEquals("Hacker News", service.name)
    }

    @Test
    fun id_isCorrect() {
        assertEquals("hackernews", service.id)
    }
}
