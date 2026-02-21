package com.feedflow.data.repository

import com.feedflow.data.local.encryption.EncryptionHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UserRepositoryTest {

    private lateinit var mockEncryptionHelper: EncryptionHelper
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        mockEncryptionHelper = mock()
        userRepository = UserRepository(mockEncryptionHelper)
    }

    @Test
    fun isLoggedIn_returnsTrueWhenCookiesExist() {
        whenever(mockEncryptionHelper.hasCookies("hackernews")).thenReturn(true)

        val result = userRepository.isLoggedIn("hackernews")

        assertTrue(result)
    }

    @Test
    fun isLoggedIn_returnsFalseWhenNoCookies() {
        whenever(mockEncryptionHelper.hasCookies("v2ex")).thenReturn(false)

        val result = userRepository.isLoggedIn("v2ex")

        assertFalse(result)
    }

    @Test
    fun getLoginStatusMap_returnsMapForAllSites() {
        whenever(mockEncryptionHelper.hasCookies("hackernews")).thenReturn(true)
        whenever(mockEncryptionHelper.hasCookies("linux_do")).thenReturn(false)
        whenever(mockEncryptionHelper.hasCookies("v2ex")).thenReturn(true)
        whenever(mockEncryptionHelper.hasCookies("4d4y")).thenReturn(false)
        whenever(mockEncryptionHelper.hasCookies("zhihu")).thenReturn(false)
        whenever(mockEncryptionHelper.hasCookies("nodeseek")).thenReturn(false)
        whenever(mockEncryptionHelper.hasCookies("2libra")).thenReturn(false)

        val map = userRepository.getLoginStatusMap()

        assertTrue(map.containsKey("hackernews"))
        assertTrue(map["hackernews"]!!)
        assertFalse(map["linux_do"]!!)
        assertTrue(map["v2ex"]!!)
        assertFalse(map.containsKey("rss"))
    }

    @Test
    fun saveCookies_delegatesToEncryptionHelper() {
        val siteId = "hackernews"
        val cookiesJson = "{\"cookies\":[]}"

        userRepository.saveCookies(siteId, cookiesJson)

        verify(mockEncryptionHelper).saveCookies(siteId, cookiesJson)
    }

    @Test
    fun getCookies_returnsFromEncryptionHelper() {
        val siteId = "hackernews"
        val cookiesJson = "{\"cookies\":[]}"
        whenever(mockEncryptionHelper.getCookies(siteId)).thenReturn(cookiesJson)

        val result = userRepository.getCookies(siteId)

        assertEquals(cookiesJson, result)
    }

    @Test
    fun getCookies_returnsNullWhenNone() {
        whenever(mockEncryptionHelper.getCookies("v2ex")).thenReturn(null)

        val result = userRepository.getCookies("v2ex")

        assertNull(result)
    }

    @Test
    fun logout_removesCookies() {
        val siteId = "hackernews"

        userRepository.logout(siteId)

        verify(mockEncryptionHelper).removeCookies(siteId)
    }

    @Test
    fun logoutAll_removesAllSiteCookies() = runTest {
        userRepository.logoutAll()

        verify(mockEncryptionHelper).removeCookies("hackernews")
        verify(mockEncryptionHelper).removeCookies("linux_do")
        verify(mockEncryptionHelper).removeCookies("v2ex")
        verify(mockEncryptionHelper).removeCookies("4d4y")
        verify(mockEncryptionHelper).removeCookies("zhihu")
        verify(mockEncryptionHelper).removeCookies("nodeseek")
        verify(mockEncryptionHelper).removeCookies("2libra")
        verify(mockEncryptionHelper).removeCookies("rss")
    }

    @Test
    fun saveCredential_delegatesToEncryptionHelper() {
        val key = "api_key"
        val value = "secret123"

        userRepository.saveCredential(key, value)

        verify(mockEncryptionHelper).saveCredential(key, value)
    }

    @Test
    fun getCredential_returnsFromEncryptionHelper() {
        val key = "api_key"
        val value = "secret123"
        whenever(mockEncryptionHelper.getCredential(key)).thenReturn(value)

        val result = userRepository.getCredential(key)

        assertEquals(value, result)
    }

    @Test
    fun removeCredential_delegatesToEncryptionHelper() {
        val key = "api_key"

        userRepository.removeCredential(key)

        verify(mockEncryptionHelper).removeCredential(key)
    }
}
