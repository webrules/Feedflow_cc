package com.feedflow.domain.service

import com.feedflow.data.local.encryption.EncryptionHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class FourD4YServiceTest {

    @MockK
    lateinit var mockClient: OkHttpClient

    @MockK
    lateinit var mockEncryptionHelper: EncryptionHelper

    private lateinit var service: FourD4YService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = FourD4YService(mockClient, mockEncryptionHelper)
    }

    @Test
    fun `concurrent updateCookies should not lose cookies`() = runBlocking {
        // Given
        val cookieStore = AtomicReference<String>("")
        val counter = AtomicInteger(0)
        val numberOfThreads = 20

        // Mock EncryptionHelper
        // Use synchronized block to simulate thread-safety of individual get/save operations,
        // but NOT the read-modify-write cycle which is the problem.
        every { mockEncryptionHelper.getCookies("4d4y") } answers {
            // Add slight delay to make race condition more likely during read
            Thread.sleep(1)
            cookieStore.get()
        }
        every { mockEncryptionHelper.saveCookies("4d4y", any()) } answers {
            // Simulate save
            val newCookies = secondArg<String>()
            // Add slight delay to make race condition more likely during write
            Thread.sleep(1)
            cookieStore.set(newCookies)
        }

        // Mock Client to return unique cookies per call
        every { mockClient.newCall(any()) } answers {
            val idx = counter.incrementAndGet()
            val mockCall = mockk<Call>()
            val cookieName = "c$idx"
            val newCookieHeader = "$cookieName=v; Path=/; HttpOnly"

            val response = Response.Builder()
                .request(Request.Builder().url("https://www.4d4y.com/forum/index.php").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Set-Cookie", newCookieHeader)
                .body("".toResponseBody("text/html; charset=GBK".toMediaTypeOrNull()))
                .build()

            every { mockCall.execute() } returns response
            mockCall
        }

        // When: Launch concurrent requests
        val jobs = (1..numberOfThreads).map {
            launch(Dispatchers.IO) {
                try {
                    service.fetchCategories()
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }

        jobs.joinAll()

        // Then
        val finalCookies = cookieStore.get()
        println("Final cookies: $finalCookies")

        // Assert that ALL cookies made it into the store
        // If there is a race condition, some c<i> cookies will be missing.
        val missingCookies = (1..numberOfThreads).filter { !finalCookies.contains("c$it=v") }

        if (missingCookies.isNotEmpty()) {
            println("Missing cookies: $missingCookies")
        }

        assertTrue("Expected all cookies to be present, but missing: $missingCookies", missingCookies.isEmpty())
    }
}
