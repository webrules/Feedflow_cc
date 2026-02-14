package com.feedflow.ui.home

import app.cash.turbine.test
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SiteListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: SiteListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock()
        preferencesManager = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sites_returnsEnabledSites() = runTest {
        // Mock community visibility: only HackerNews enabled (plus RSS which is always enabled)
        whenever(preferencesManager.communityVisibility).thenReturn(flowOf("hackernews"))
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())

        viewModel = SiteListViewModel(userRepository, preferencesManager)

        viewModel.sites.test {
            // Initial value (all sites)
            val initial = awaitItem()

            // Filtered value
            val filtered = awaitItem()

            assertTrue(filtered.contains(ForumSite.RSS))
            assertTrue(filtered.any { it.id == "hackernews" })
            assertTrue(filtered.none { it == ForumSite.V2EX })
        }
    }

    @Test
    fun loginStatus_updatesFromRepository() = runTest {
        whenever(preferencesManager.communityVisibility).thenReturn(flowOf(""))
        val loginMap = mapOf("hackernews" to true)
        whenever(userRepository.getLoginStatusMap()).thenReturn(loginMap)

        viewModel = SiteListViewModel(userRepository, preferencesManager)

        // Wait for coroutines to run
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loginStatus.test {
            val status = awaitItem()
            assertEquals(true, status["hackernews"])
        }

        assertEquals(true, viewModel.isLoggedIn("hackernews"))
    }
}
