package com.feedflow.domain.service

import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.remote.api.DiscourseApi
import com.feedflow.data.remote.dto.DiscourseResponse
import com.feedflow.data.remote.dto.DiscourseCategoryList
import com.feedflow.data.remote.dto.DiscourseCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DiscourseServiceTest {

    private lateinit var mockApi: DiscourseApi
    private lateinit var service: DiscourseService

    @Before
    fun setup() {
        mockApi = mock()
        service = DiscourseService(mockApi)
    }

    @Test
    fun fetchCategories_returnsFromApi() = runBlocking {
        val response = DiscourseResponse(
            categoryList = DiscourseCategoryList(
                categories = listOf(
                    DiscourseCategory(id = 1, slug = "general", name = "General", description = "General topics", topicCount = 100, postCount = 500),
                    DiscourseCategory(id = 2, slug = "tech", name = "Tech", description = "Technology", topicCount = 50, postCount = 200)
                )
            )
        )
        whenever(mockApi.getCategories()).thenReturn(response)

        val categories = service.fetchCategories()

        assertEquals(2, categories.size)
        assertEquals("general", categories[0].id)
        assertEquals("General", categories[0].name)
    }

    @Test
    fun fetchCategories_emptyResponse_returnsEmptyList() = runBlocking {
        val response = DiscourseResponse(categoryList = null)
        whenever(mockApi.getCategories()).thenReturn(response)

        val categories = service.fetchCategories()

        assertTrue(categories.isEmpty())
    }

    @Test
    fun getWebURL_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "12345",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("general", "General", "", "linux_do"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://linux.do/t/12345", url)
    }

    @Test
    fun supportsPosting_returnsTrue() {
        assertTrue(service.supportsPosting())
    }

    @Test
    fun requiresLogin_returnsTrue() {
        assertTrue(service.requiresLogin())
    }

    @Test
    fun name_isCorrect() {
        assertEquals("Linux.do", service.name)
    }

    @Test
    fun id_isCorrect() {
        assertEquals("linux_do", service.id)
    }

    @Test
    fun postComment_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.postComment("1", "general", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun createThread_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.createThread("general", "title", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }
}
