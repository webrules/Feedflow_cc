package com.feedflow.ui.threads

import app.cash.turbine.test
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import com.feedflow.data.repository.CacheRepository
import com.feedflow.data.repository.CommunityRepository
import com.feedflow.domain.service.ForumService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cacheRepository: CacheRepository
    private lateinit var communityRepository: CommunityRepository
    private lateinit var forumService: ForumService
    private lateinit var viewModel: ThreadListViewModel

    // Helper to create threads
    private fun createThread(id: String, title: String) = ForumThread(
        id = id,
        title = title,
        content = "Content",
        author = User("u1", "User", ""),
        community = Community("c1", "Community", "", "test_service"),
        timeAgo = "time",
        likeCount = 0,
        commentCount = 0
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        cacheRepository = mock()
        communityRepository = mock()
        forumService = mock()
        whenever(forumService.id).thenReturn("test_service")

        val services = mapOf("test_service" to forumService)
        viewModel = ThreadListViewModel(cacheRepository, communityRepository, services)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadThreadsBySiteAndCommunity_loadsFromCacheThenNetwork() = runTest {
        // Setup
        val communityId = "c1"
        val cacheKey = "test_service_c1_1"
        val cachedThreads = listOf(createThread("1", "Cached"))
        val freshThreads = listOf(createThread("2", "Fresh"))

        whenever(communityRepository.getCommunitiesByServiceOnce("test_service")).thenReturn(emptyList())
        whenever(forumService.fetchCategories()).thenReturn(listOf(Community(communityId, "Name", "", "test_service")))

        whenever(cacheRepository.getCachedTopics(cacheKey)).thenReturn(cachedThreads)
        whenever(forumService.fetchCategoryThreads(any(), any(), eq(1))).thenReturn(freshThreads)

        viewModel.threads.test {
            // Initial empty state
            assertEquals(emptyList<ForumThread>(), awaitItem())

            // Act
            viewModel.loadThreadsBySiteAndCommunity("test_service", communityId)

            // Cached state
            val cached = awaitItem()
            assertEquals(1, cached.size)
            assertEquals("Cached", cached[0].title)

            // Fresh state (after network call)
            val fresh = awaitItem()
            assertEquals(1, fresh.size)
            assertEquals("Fresh", fresh[0].title)
        }

        verify(cacheRepository).saveCachedTopics(eq(cacheKey), any())
    }

    @Test
    fun loadMore_appendsThreads() = runTest {
        // Setup initial state
        val communityId = "c1"
        val initialThreads = listOf(createThread("1", "Thread 1"))
        val nextThreads = listOf(createThread("2", "Thread 2"))

        // Mock setup for initial load
        whenever(communityRepository.getCommunitiesByServiceOnce("test_service")).thenReturn(emptyList())
        whenever(forumService.fetchCategories()).thenReturn(listOf(Community(communityId, "Name", "", "test_service")))
        whenever(cacheRepository.getCachedTopics(any())).thenReturn(null)
        whenever(forumService.fetchCategoryThreads(any(), any(), eq(1))).thenReturn(initialThreads)

        // Mock setup for load more (page 2)
        whenever(forumService.fetchCategoryThreads(any(), any(), eq(2))).thenReturn(nextThreads)

        // Act - Load initial
        viewModel.loadThreadsBySiteAndCommunity("test_service", communityId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.threads.test {
            // Should start with initial loaded threads
            val initial = awaitItem()
            assertEquals(1, initial.size)
            assertEquals("Thread 1", initial[0].title)

            // Act - Load more
            viewModel.loadMore()

            // Should update with combined threads
            val combined = awaitItem()
            assertEquals(2, combined.size)
            assertEquals("Thread 1", combined[0].title)
            assertEquals("Thread 2", combined[1].title)
        }
    }
}
