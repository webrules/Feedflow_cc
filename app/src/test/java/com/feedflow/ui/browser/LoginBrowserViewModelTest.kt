package com.feedflow.ui.browser

import com.feedflow.data.local.encryption.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class LoginBrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var encryptionHelper: EncryptionHelper
    private lateinit var viewModel: LoginBrowserViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        encryptionHelper = mock()
        viewModel = LoginBrowserViewModel(encryptionHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveCookies_delegatesToEncryptionHelper() = runTest {
        viewModel.saveCookies("hackernews", "session=abc123")

        verify(encryptionHelper).saveCookies("hackernews", "session=abc123")
    }

    @Test
    fun saveCookies_emptyCookies_stillSaves() = runTest {
        viewModel.saveCookies("v2ex", "")

        verify(encryptionHelper).saveCookies("v2ex", "")
    }
}
