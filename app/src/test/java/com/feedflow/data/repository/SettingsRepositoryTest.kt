package com.feedflow.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var database: com.feedflow.data.local.db.FeedflowDatabase
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            com.feedflow.data.local.db.FeedflowDatabase::class.java
        ).allowMainThreadQueries().build()

        settingsRepository = SettingsRepository(database.settingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveSetting_andGetSetting() = runTest {
        settingsRepository.saveSetting("test_key", "test_value")

        val result = settingsRepository.getSetting("test_key")

        assertEquals("test_value", result)
    }

    @Test
    fun getSetting_returnsNullWhenNotFound() = runTest {
        val result = settingsRepository.getSetting("nonexistent_key")

        assertNull(result)
    }

    @Test
    fun saveSetting_overwritesExisting() = runTest {
        settingsRepository.saveSetting("key", "value1")
        settingsRepository.saveSetting("key", "value2")

        val result = settingsRepository.getSetting("key")

        assertEquals("value2", result)
    }

    @Test
    fun deleteSetting_removesSetting() = runTest {
        settingsRepository.saveSetting("key", "value")
        settingsRepository.deleteSetting("key")

        val result = settingsRepository.getSetting("key")

        assertNull(result)
    }

    @Test
    fun getSettingFlow_emitsUpdates() = runTest {
        val flow = settingsRepository.getSettingFlow("flow_key")

        settingsRepository.saveSetting("flow_key", "flow_value")

        val result = flow.first()
        assertEquals("flow_value", result)
    }

    @Test
    fun deleteAll_removesAllSettings() = runTest {
        settingsRepository.saveSetting("key1", "value1")
        settingsRepository.saveSetting("key2", "value2")

        settingsRepository.deleteAll()

        assertNull(settingsRepository.getSetting("key1"))
        assertNull(settingsRepository.getSetting("key2"))
    }
}
