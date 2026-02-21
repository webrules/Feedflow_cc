package com.feedflow.ui.communities

import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumSite
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CommunitiesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var communityRepository: CommunityRepository
    private lateinit var forumService: ForumService
    private lateinit var viewModel: CommunitiesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        communityRepository = mock()
        forumService = mock()
        whenever(forumService.id).thenReturn("hackernews")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadCommunities_loadsFromService() = runTest {
        val communities = listOf(
            Community("c1", "Community 1", "Desc", "hackernews"),
            Community("c2", "Community 2", "Desc", "hackernews")
        )
        whenever(communityRepository.getCommunitiesByServiceOnce("hackernews")).thenReturn(emptyList())
        whenever(forumService.fetchCategories()).thenReturn(communities)

        val services = mapOf<String, ForumService>("hackernews" to forumService)
        viewModel = CommunitiesViewModel(communityRepository, services)

        viewModel.loadCommunities(ForumSite.HACKER_NEWS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.communities.value.size)
    }

    @Test
    fun isLoading_isFalseAfterLoad() = runTest {
        whenever(communityRepository.getCommunitiesByServiceOnce("hackernews")).thenReturn(emptyList())
        whenever(forumService.fetchCategories()).thenReturn(emptyList())

        val services = mapOf<String, ForumService>("hackernews" to forumService)
        viewModel = CommunitiesViewModel(communityRepository, services)

        viewModel.loadCommunities(ForumSite.HACKER_NEWS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun error_isNullAfterSuccessfulLoad() = runTest {
        whenever(communityRepository.getCommunitiesByServiceOnce("hackernews")).thenReturn(emptyList())
        whenever(forumService.fetchCategories()).thenReturn(emptyList())

        val services = mapOf<String, ForumService>("hackernews" to forumService)
        viewModel = CommunitiesViewModel(communityRepository, services)

        viewModel.loadCommunities(ForumSite.HACKER_NEWS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.error.value)
    }
}
