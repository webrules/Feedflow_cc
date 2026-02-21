package com.feedflow.ui.settings

import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.repository.CoverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var coverRepository: CoverRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesManager = mock()
        coverRepository = mock()

        whenever(preferencesManager.isDarkMode).thenReturn(flowOf(false))
        whenever(preferencesManager.language).thenReturn(flowOf("en"))
        whenever(preferencesManager.geminiApiKey).thenReturn(flowOf(null))
        whenever(preferencesManager.communityVisibility).thenReturn(flowOf(""))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleDarkMode_updatesPreference() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.toggleDarkMode()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesManager).setDarkMode(true)
    }

    @Test
    fun setDarkMode_updatesPreference() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.setDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesManager).setDarkMode(true)
    }

    @Test
    fun toggleLanguage_switchesFromEnToZh() = runTest {
        whenever(preferencesManager.language).thenReturn(flowOf("en"))

        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.toggleLanguage()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesManager).setLanguage("zh")
    }

    @Test
    fun setLanguage_updatesPreference() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.setLanguage("zh")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesManager).setLanguage("zh")
    }

    @Test
    fun setGeminiApiKey_updatesPreference() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.setGeminiApiKey("test-key")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesManager).setGeminiApiKey("test-key")
    }

    @Test
    fun clearOldCache_callsRepository() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.clearOldCache()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(coverRepository).deleteOlderThanOneWeek()
    }

    @Test
    fun dismissCacheClearResult_clearsResult() = runTest {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)
        viewModel.clearOldCache()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissCacheClearResult()

        assertTrue(viewModel.cacheClearResult.value == null)
    }

    @Test
    fun optionalSites_containsExpectedSites() {
        viewModel = SettingsViewModel(preferencesManager, coverRepository)

        assertTrue(viewModel.optionalSites.any { it.id == "hackernews" })
        assertTrue(viewModel.optionalSites.any { it.id == "v2ex" })
        assertFalse(viewModel.optionalSites.any { it.id == "rss" })
    }
}
