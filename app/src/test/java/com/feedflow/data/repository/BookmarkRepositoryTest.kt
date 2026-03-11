package com.feedflow.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feedflow.data.local.db.FeedflowDatabase
import com.feedflow.data.local.db.dao.BookmarkDao
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkRepositoryTest {

    private lateinit var database: FeedflowDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var bookmarkRepository: BookmarkRepository

    private fun createThread(id: String) = ForumThread(
        id = id,
        title = "Test Thread $id",
        content = "Content",
        author = User("u1", "User", ""),
        community = Community("c1", "Community", "", "test_service"),
        timeAgo = "1h",
        likeCount = 10,
        commentCount = 5
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FeedflowDatabase::class.java
        ).allowMainThreadQueries().build()

        bookmarkDao = database.bookmarkDao()
        bookmarkRepository = BookmarkRepository(bookmarkDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun toggleBookmark_addsBookmark() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        bookmarkRepository.toggleBookmark(thread, serviceId)

        val isBookmarked = bookmarkRepository.isBookmarked(thread.id, serviceId)
        assertTrue(isBookmarked)
    }

    @Test
    fun toggleBookmark_removesBookmark() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        bookmarkRepository.toggleBookmark(thread, serviceId)
        bookmarkRepository.toggleBookmark(thread, serviceId)

        val isBookmarked = bookmarkRepository.isBookmarked(thread.id, serviceId)
        assertFalse(isBookmarked)
    }

    @Test
    fun addBookmark_addsBookmark() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        bookmarkRepository.addBookmark(thread, serviceId)

        val isBookmarked = bookmarkRepository.isBookmarked(thread.id, serviceId)
        assertTrue(isBookmarked)
    }

    @Test
    fun addBookmark_duplicateId_replacesBookmark() = runTest {
        val thread1 = createThread("1").copy(title = "First")
        val thread2 = createThread("1").copy(title = "Second")
        val serviceId = "test_service"

        bookmarkRepository.addBookmark(thread1, serviceId)
        bookmarkRepository.addBookmark(thread2, serviceId)

        val bookmarks = bookmarkRepository.getAllBookmarksOnce()
        assertEquals(1, bookmarks.size)
        assertEquals("Second", bookmarks[0].first.title)
    }

    @Test
    fun removeBookmark_removesBookmark() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        bookmarkRepository.addBookmark(thread, serviceId)
        bookmarkRepository.removeBookmark(thread.id, serviceId)

        val isBookmarked = bookmarkRepository.isBookmarked(thread.id, serviceId)
        assertFalse(isBookmarked)
    }

    @Test
    fun getAllBookmarksOnce_returnsAllBookmarks() = runTest {
        val thread1 = createThread("1")
        val thread2 = createThread("2")
        val serviceId = "test_service"

        bookmarkRepository.addBookmark(thread1, serviceId)
        bookmarkRepository.addBookmark(thread2, serviceId)

        val bookmarks = bookmarkRepository.getAllBookmarksOnce()

        assertEquals(2, bookmarks.size)
    }

    @Test
    fun getAllBookmarks_returnsFlow() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        bookmarkRepository.addBookmark(thread, serviceId)

        val bookmarks = bookmarkRepository.getAllBookmarks().first()

        assertEquals(1, bookmarks.size)
    }

    @Test
    fun isBookmarkedFlow_returnsCorrectValue() = runTest {
        val thread = createThread("1")
        val serviceId = "test_service"

        val beforeAdd = bookmarkRepository.isBookmarkedFlow(thread.id, serviceId).first()
        assertFalse(beforeAdd)

        bookmarkRepository.addBookmark(thread, serviceId)

        val afterAdd = bookmarkRepository.isBookmarkedFlow(thread.id, serviceId).first()
        assertTrue(afterAdd)
    }

    @Test
    fun toggleUrlBookmark_addsUrlBookmark() = runTest {
        val url = "http://example.com/article"
        val title = "Article Title"

        bookmarkRepository.toggleUrlBookmark(url, title)

        val isBookmarked = bookmarkRepository.isUrlBookmarked(url)
        assertTrue(isBookmarked)
    }

    @Test
    fun toggleUrlBookmark_removesUrlBookmark() = runTest {
        val url = "http://example.com/article"
        val title = "Article Title"

        bookmarkRepository.toggleUrlBookmark(url, title)
        bookmarkRepository.toggleUrlBookmark(url, title)

        val isBookmarked = bookmarkRepository.isUrlBookmarked(url)
        assertFalse(isBookmarked)
    }

    @Test
    fun addUrlBookmark_addsBookmark() = runTest {
        val url = "http://example.com/article"
        val title = "Article Title"

        bookmarkRepository.addUrlBookmark(url, title)

        val isBookmarked = bookmarkRepository.isUrlBookmarked(url)
        assertTrue(isBookmarked)
    }

    @Test
    fun removeUrlBookmark_removesBookmark() = runTest {
        val url = "http://example.com/article"
        val title = "Article Title"

        bookmarkRepository.addUrlBookmark(url, title)
        bookmarkRepository.removeUrlBookmark(url)

        val isBookmarked = bookmarkRepository.isUrlBookmarked(url)
        assertFalse(isBookmarked)
    }

    @Test
    fun getAllUrlBookmarksOnce_returnsAll() = runTest {
        bookmarkRepository.addUrlBookmark("http://example.com/1", "Title 1")
        bookmarkRepository.addUrlBookmark("http://example.com/2", "Title 2")

        val bookmarks = bookmarkRepository.getAllUrlBookmarksOnce()

        assertEquals(2, bookmarks.size)
    }

    @Test
    fun differentServiceIds_separateBookmarks() = runTest {
        val thread = createThread("1")

        bookmarkRepository.addBookmark(thread, "service1")
        bookmarkRepository.addBookmark(thread, "service2")

        val bookmarks = bookmarkRepository.getAllBookmarksOnce()
        assertEquals(2, bookmarks.size)
    }
}
