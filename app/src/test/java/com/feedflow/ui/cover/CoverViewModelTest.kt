package com.feedflow.ui.cover

import com.feedflow.data.repository.CoverPageData
import com.feedflow.data.repository.CoverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CoverViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var coverRepository: CoverRepository
    private lateinit var viewModel: CoverViewModel

    private fun createCoverData(fromCache: Boolean = false) = CoverPageData(
        summary = "Test summary",
        summaryEn = "English summary",
        summaryCn = "中文摘要",
        hnSummaryEn = "HN EN",
        hnSummaryCn = "HN CN",
        v2exSummaryEn = "V2EX EN",
        v2exSummaryCn = "V2EX CN",
        fourD4ySummaryEn = "4D4Y EN",
        fourD4ySummaryCn = "4D4Y CN",
        hnThreads = emptyList(),
        v2exThreads = emptyList(),
        fourD4yThreads = emptyList(),
        createdAt = System.currentTimeMillis(),
        fromCache = fromCache
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coverRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsCover() = runTest {
        val data = createCoverData()
        whenever(coverRepository.getCoverPage(false)).thenReturn(data)

        viewModel = CoverViewModel(coverRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CoverUiState.Success)
    }

    @Test
    fun loadCover_forceRefresh_loadsFresh() = runTest {
        val data = createCoverData()
        whenever(coverRepository.getCoverPage(true)).thenReturn(data)

        viewModel = CoverViewModel(coverRepository)
        viewModel.loadCover(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CoverUiState.Success)
    }

    @Test
    fun loadCover_error_setsErrorState() = runTest {
        whenever(coverRepository.getCoverPage(false)).thenThrow(RuntimeException("Network error"))

        viewModel = CoverViewModel(coverRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CoverUiState.Error)
    }
}
