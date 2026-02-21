package com.feedflow.ui.browser

import com.feedflow.data.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InAppBrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var viewModel: InAppBrowserViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bookmarkRepository = mock()
        viewModel = InAppBrowserViewModel(bookmarkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_checksBookmarkStatus() = runTest {
        whenever(bookmarkRepository.isUrlBookmarked("https://example.com")).thenReturn(false)

        viewModel.init("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isBookmarked.value)
    }

    @Test
    fun init_withBookmarkedUrl_setsBookmarkedTrue() = runTest {
        whenever(bookmarkRepository.isUrlBookmarked("https://example.com")).thenReturn(true)

        viewModel.init("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isBookmarked.value)
    }

    @Test
    fun updatePageInfo_updatesUrlAndChecksBookmark() = runTest {
        whenever(bookmarkRepository.isUrlBookmarked("https://newurl.com")).thenReturn(true)

        viewModel.updatePageInfo("https://newurl.com", "New Page")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isBookmarked.value)
    }

    @Test
    fun toggleBookmark_togglesState() = runTest {
        whenever(bookmarkRepository.isUrlBookmarked("https://example.com")).thenReturn(false)

        viewModel.init("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isBookmarked.value)
        verify(bookmarkRepository).toggleUrlBookmark("https://example.com", "")
    }

    @Test
    fun toggleBookmark_usesCurrentTitle() = runTest {
        whenever(bookmarkRepository.isUrlBookmarked("https://example.com")).thenReturn(false)

        viewModel.updatePageInfo("https://example.com", "Page Title")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(bookmarkRepository).toggleUrlBookmark("https://example.com", "Page Title")
    }
}
