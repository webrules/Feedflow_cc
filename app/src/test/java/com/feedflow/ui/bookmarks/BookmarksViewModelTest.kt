package com.feedflow.ui.bookmarks

import com.feedflow.data.local.db.entity.UrlBookmarkEntity
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import com.feedflow.data.repository.BookmarkRepository
import com.feedflow.domain.service.ForumService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var forumService: ForumService
    private lateinit var viewModel: BookmarksViewModel

    private fun createThread(id: String) = ForumThread(
        id = id,
        title = "Thread $id",
        content = "Content",
        author = User("u1", "User", ""),
        community = Community("c1", "Community", "", "test_service"),
        timeAgo = "1h",
        likeCount = 10,
        commentCount = 5
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bookmarkRepository = mock()
        forumService = mock()
        whenever(forumService.id).thenReturn("test_service")

        val services = mapOf<String, ForumService>("test_service" to forumService)
        viewModel = BookmarksViewModel(bookmarkRepository, services)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadBookmarks_loadsThreadBookmarks() = runTest {
        val threads = listOf(createThread("1") to "test_service")
        whenever(bookmarkRepository.getAllBookmarksOnce()).thenReturn(threads)
        whenever(bookmarkRepository.getAllUrlBookmarksOnce()).thenReturn(emptyList())

        viewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.threadBookmarks.value.size)
    }

    @Test
    fun loadBookmarks_loadsUrlBookmarks() = runTest {
        val urlBookmarks = listOf(
            UrlBookmarkEntity(url = "https://example.com", title = "Example", timestamp = System.currentTimeMillis())
        )
        whenever(bookmarkRepository.getAllBookmarksOnce()).thenReturn(emptyList())
        whenever(bookmarkRepository.getAllUrlBookmarksOnce()).thenReturn(urlBookmarks)

        viewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.urlBookmarks.value.size)
    }

    @Test
    fun removeThreadBookmark_removesFromList() = runTest {
        val thread = createThread("1")
        whenever(bookmarkRepository.getAllBookmarksOnce()).thenReturn(listOf(thread to "test_service"))
        whenever(bookmarkRepository.getAllUrlBookmarksOnce()).thenReturn(emptyList())

        viewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeThreadBookmark(thread, "test_service")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.threadBookmarks.value.isEmpty())
    }

    @Test
    fun removeUrlBookmark_removesFromList() = runTest {
        val urlBookmark = UrlBookmarkEntity(
            url = "https://example.com",
            title = "Example",
            timestamp = System.currentTimeMillis()
        )
        whenever(bookmarkRepository.getAllBookmarksOnce()).thenReturn(emptyList())
        whenever(bookmarkRepository.getAllUrlBookmarksOnce()).thenReturn(listOf(urlBookmark))

        viewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeUrlBookmark("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.urlBookmarks.value.isEmpty())
    }

    @Test
    fun getService_returnsCorrectService() {
        val service = viewModel.getService("test_service")
        assertNotNull(service)
    }

    @Test
    fun getService_returnsNullForUnknownService() {
        val service = viewModel.getService("unknown")
        assertNull(service)
    }

    @Test
    fun isLoading_isFalseAfterLoad() = runTest {
        whenever(bookmarkRepository.getAllBookmarksOnce()).thenReturn(emptyList())
        whenever(bookmarkRepository.getAllUrlBookmarksOnce()).thenReturn(emptyList())

        viewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
    }
}
