package com.feedflow.data.local.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feedflow.data.local.encryption.EncryptionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.ExperimentalCoroutinesApi

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesManagerTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: Context
    private lateinit var encryptionHelper: EncryptionHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionHelper = EncryptionHelper(context)
        preferencesManager = PreferencesManager(context, encryptionHelper)
    }

    @Test
    fun setDarkMode_savesValue() = runTest {
        preferencesManager.setDarkMode(true)
        val isDarkMode = preferencesManager.isDarkMode.first()
        assertEquals(true, isDarkMode)

        preferencesManager.setDarkMode(false)
        val isDarkMode2 = preferencesManager.isDarkMode.first()
        assertEquals(false, isDarkMode2)
    }

    @Test
    fun setLanguage_savesValue() = runTest {
        preferencesManager.setLanguage("zh")
        val language = preferencesManager.language.first()
        assertEquals("zh", language)
    }

    @Test
    fun setGeminiApiKey_savesAndRemovesValue() = runTest {
        preferencesManager.setGeminiApiKey("test_key")
        val key = preferencesManager.geminiApiKey.first()
        assertEquals("test_key", key)

        preferencesManager.setGeminiApiKey(null)
        val key2 = preferencesManager.geminiApiKey.first()
        assertNull(key2)
    }
}
