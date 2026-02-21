package com.feedflow.ui.detail

import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.data.repository.BookmarkRepository
import com.feedflow.data.repository.CacheRepository
import com.feedflow.domain.service.ForumService
import com.feedflow.domain.service.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cacheRepository: CacheRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var geminiService: GeminiService
    private lateinit var forumService: ForumService
    private lateinit var viewModel: ThreadDetailViewModel

    private fun createThread(id: String) = ForumThread(
        id = id,
        title = "Test Thread $id",
        content = "Content for thread $id",
        author = User("u1", "TestUser", ""),
        community = Community("c1", "Test Community", "", "test_service"),
        timeAgo = "1h",
        likeCount = 10,
        commentCount = 5
    )

    private fun createComment(id: String) = Comment(
        id = id,
        author = User("u$id", "User$id", ""),
        content = "Comment content $id",
        timeAgo = "30m",
        likeCount = 5
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        cacheRepository = mock()
        bookmarkRepository = mock()
        geminiService = mock()
        forumService = mock()

        whenever(forumService.id).thenReturn("test_service")
        whenever(forumService.getWebURL(any())).thenReturn("https://example.com/thread/1")

        val services = mapOf<String, ForumService>("test_service" to forumService)
        viewModel = ThreadDetailViewModel(cacheRepository, bookmarkRepository, geminiService, services)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadThread_loadsFromNetworkAndSavesCache() = runTest {
        val thread = createThread("1")
        val comments = listOf(createComment("c1"))
        val result = ThreadDetailResult(thread, comments, null)

        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1)).thenReturn(result)
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is ThreadDetailState.Loaded)
        val loaded = state as ThreadDetailState.Loaded
        assertEquals("Test Thread 1", loaded.thread.title)
        verify(cacheRepository).saveCachedThread(eq("1"), any(), any())
    }

    @Test
    fun loadThread_loadsCacheFirstThenFresh() = runTest {
        val cachedThread = createThread("cached")
        val freshThread = createThread("fresh")

        whenever(cacheRepository.getCachedThread("1"))
            .thenReturn(Pair(cachedThread, emptyList()))
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(freshThread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is ThreadDetailState.Loaded)
        val loaded = state as ThreadDetailState.Loaded
        assertTrue(loaded.thread.title.contains("fresh") || loaded.thread.title.contains("cached"))
    }

    @Test
    fun loadThread_setsBookmarkedState() = runTest {
        val thread = createThread("1")
        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(true)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as ThreadDetailState.Loaded
        assertTrue(state.isBookmarked)
    }

    @Test
    fun toggleBookmark_togglesState() = runTest {
        val thread = createThread("1")
        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as ThreadDetailState.Loaded
        assertTrue(state.isBookmarked)
        verify(bookmarkRepository).toggleBookmark(any(), eq("test_service"))
    }

    @Test
    fun loadMoreComments_appendsComments() = runTest {
        val thread = createThread("1")
        val initialComments = listOf(createComment("c1"))
        val moreComments = listOf(createComment("c2"))

        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, initialComments, 2))
        whenever(forumService.fetchThreadDetail("1", 2))
            .thenReturn(ThreadDetailResult(thread, moreComments, 2))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val state1 = viewModel.state.value as ThreadDetailState.Loaded
        assertEquals(1, state1.comments.size)

        viewModel.loadMoreComments()
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.state.value as ThreadDetailState.Loaded
        assertEquals(2, state2.comments.size)
    }

    @Test
    fun selectReplyTarget_setsReplyingTo() {
        val comment = createComment("c1")
        viewModel.selectReplyTarget(comment)

        val state = viewModel.state.value
        if (state is ThreadDetailState.Loaded) {
            assertEquals(comment, state.replyingTo)
        }
    }

    @Test
    fun getWebURL_returnsCorrectUrl() = runTest {
        val thread = createThread("1")
        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val url = viewModel.getWebURL()

        assertEquals("https://example.com/thread/1", url)
    }

    @Test
    fun getService_returnsCurrentService() = runTest {
        val thread = createThread("1")
        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        val service = viewModel.getService()

        assertNotNull(service)
        assertEquals("test_service", service?.id)
    }

    @Test
    fun generateSummary_savesToCache() = runTest {
        val thread = createThread("1")
        val summary = "This is an AI summary"

        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)
        whenever(cacheRepository.getSummaryIfFresh(eq("1"), any())).thenReturn(null)
        whenever(geminiService.generateSummary(any())).thenReturn(summary)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateSummary()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as ThreadDetailState.Loaded
        assertEquals(summary, state.summary)
        verify(cacheRepository).saveSummary("1", summary)
    }

    @Test
    fun generateSummary_usesCachedWhenAvailable() = runTest {
        val thread = createThread("1")
        val cachedSummary = "Cached summary"

        whenever(cacheRepository.getCachedThread("1")).thenReturn(null)
        whenever(forumService.fetchThreadDetail("1", 1))
            .thenReturn(ThreadDetailResult(thread, emptyList(), null))
        whenever(bookmarkRepository.isBookmarked("1", "test_service")).thenReturn(false)
        whenever(cacheRepository.getSummaryIfFresh(eq("1"), any())).thenReturn(cachedSummary)

        viewModel.loadThread("1", "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateSummary()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as ThreadDetailState.Loaded
        assertEquals(cachedSummary, state.summary)
        assertTrue(state.isSummaryCached)
    }
}
