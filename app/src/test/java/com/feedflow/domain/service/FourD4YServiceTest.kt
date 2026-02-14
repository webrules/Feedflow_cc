package com.feedflow.domain.service

import com.feedflow.data.local.encryption.EncryptionHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

class FourD4YServiceTest {

    @MockK
    lateinit var mockClient: OkHttpClient

    @MockK
    lateinit var mockEncryptionHelper: EncryptionHelper

    @MockK
    lateinit var mockCall: Call

    private lateinit var service: FourD4YService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        // Removed Log mocking to simplify diagnosis
        service = FourD4YService(mockClient, mockEncryptionHelper)
    }

    @Test
    fun `fetchCategories should update cookies when Set-Cookie header is present`() = runBlocking {
        // Given
        val oldCookies = "old_cookie=old_value"
        val newCookieHeader = "new_cookie=new_value; Path=/; HttpOnly"

        every { mockEncryptionHelper.getCookies("4d4y") } returns oldCookies
        every { mockEncryptionHelper.saveCookies("4d4y", any()) } just Runs

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://www.4d4y.com/forum/index.php").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Set-Cookie", newCookieHeader)
            .body("<html><body><a href='forumdisplay.php?fid=2'>Test Forum</a></body></html>".toResponseBody("text/html; charset=GBK".toMediaTypeOrNull()))
            .build()

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        // When
        service.fetchCategories()

        // Then
        verify(exactly = 1) {
            mockEncryptionHelper.saveCookies("4d4y", match {
                it.contains("new_cookie=new_value") && it.contains("old_cookie=old_value")
            })
        }
    }
}
