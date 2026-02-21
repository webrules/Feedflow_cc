package com.feedflow.ui.login

import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.UserRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock()
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshLoginStatus_updatesStatus() = runTest {
        val statusMap = mapOf("hackernews" to true, "v2ex" to false)
        whenever(userRepository.getLoginStatusMap()).thenReturn(statusMap)

        viewModel = LoginViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(statusMap, viewModel.loginStatus.value)
    }

    @Test
    fun isLoggedIn_returnsCorrectStatus() = runTest {
        whenever(userRepository.getLoginStatusMap()).thenReturn(mapOf("hackernews" to true))

        viewModel = LoginViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isLoggedIn("hackernews"))
        assertFalse(viewModel.isLoggedIn("v2ex"))
    }

    @Test
    fun saveCookies_savesAndRefreshes() = runTest {
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())

        viewModel = LoginViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveCookies("hackernews", "{\"cookies\":[]}")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(userRepository).saveCookies("hackernews", "{\"cookies\":[]}")
    }

    @Test
    fun logout_logsOutAndRefreshes() = runTest {
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())

        viewModel = LoginViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout("hackernews")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(userRepository).logout("hackernews")
    }

    @Test
    fun logoutAll_logsOutAll() = runTest {
        whenever(userRepository.getLoginStatusMap()).thenReturn(emptyMap())

        viewModel = LoginViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logoutAll()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(userRepository).logoutAll()
    }

    @Test
    fun getLoginUrl_hackerNews_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.HACKER_NEWS)
        assertEquals("https://news.ycombinator.com/login", url)
    }

    @Test
    fun getLoginUrl_v2ex_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.V2EX)
        assertEquals("https://v2ex.com/signin", url)
    }

    @Test
    fun getLoginUrl_linuxDo_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.LINUX_DO)
        assertEquals("https://linux.do/login", url)
    }

    @Test
    fun getLoginUrl_zhihu_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.ZHIHU)
        assertEquals("https://www.zhihu.com/signin", url)
    }

    @Test
    fun getLoginUrl_nodeSeek_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.NODE_SEEK)
        assertEquals("https://www.nodeseek.com/signIn.html", url)
    }

    @Test
    fun getLoginUrl_twoLibra_returnsCorrectUrl() {
        viewModel = LoginViewModel(userRepository)
        val url = viewModel.getLoginUrl(ForumSite.TWO_LIBRA)
        assertEquals("https://2libra.com/auth/login", url)
    }

    @Test
    fun loginableSites_excludesRss() {
        viewModel = LoginViewModel(userRepository)
        assertTrue(viewModel.loginableSites.none { it == ForumSite.RSS })
    }

    @Test
    fun clearError_clearsError() {
        viewModel = LoginViewModel(userRepository)
        viewModel.clearError()
        assertEquals(null, viewModel.error.value)
    }
}
