package com.feedflow.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feedflow.data.local.db.FeedflowDatabase
import com.feedflow.data.model.Community
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityRepositoryTest {

    private lateinit var database: FeedflowDatabase
    private lateinit var communityRepository: CommunityRepository

    private fun createCommunity(id: String, name: String) = Community(
        id = id,
        name = name,
        description = "Description for $name",
        category = "test_service"
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FeedflowDatabase::class.java
        ).allowMainThreadQueries().build()

        communityRepository = CommunityRepository(database.communityDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveCommunities_savesToDatabase() = runTest {
        val communities = listOf(
            createCommunity("1", "Community 1"),
            createCommunity("2", "Community 2")
        )

        communityRepository.saveCommunities(communities, "test_service")

        val saved = communityRepository.getCommunitiesByServiceOnce("test_service")
        assertEquals(2, saved.size)
        assertEquals("Community 1", saved[0].name)
    }

    @Test
    fun getCommunitiesByServiceOnce_returnsCorrectService() = runTest {
        val service1Communities = listOf(createCommunity("1", "Service 1 Community"))
        val service2Communities = listOf(createCommunity("2", "Service 2 Community"))

        communityRepository.saveCommunities(service1Communities, "service1")
        communityRepository.saveCommunities(service2Communities, "service2")

        val result = communityRepository.getCommunitiesByServiceOnce("service1")

        assertEquals(1, result.size)
        assertEquals("Service 1 Community", result[0].name)
    }

    @Test
    fun getCommunitiesByService_returnsFlow() = runTest {
        val communities = listOf(createCommunity("1", "Community"))

        communityRepository.saveCommunities(communities, "test_service")

        val result = communityRepository.getCommunitiesByService("test_service").first()

        assertEquals(1, result.size)
    }

    @Test
    fun saveCommunities_replacesExisting() = runTest {
        val initial = listOf(createCommunity("1", "Initial"))
        communityRepository.saveCommunities(initial, "test_service")

        val updated = listOf(createCommunity("2", "Updated"))
        communityRepository.saveCommunities(updated, "test_service")

        val result = communityRepository.getCommunitiesByServiceOnce("test_service")
        assertEquals(1, result.size)
        assertEquals("Updated", result[0].name)
    }

    @Test
    fun deleteCommunities_removesByService() = runTest {
        val communities = listOf(createCommunity("1", "Community"))
        communityRepository.saveCommunities(communities, "test_service")

        communityRepository.deleteCommunities("test_service")

        val result = communityRepository.getCommunitiesByServiceOnce("test_service")
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAll_removesAllCommunities() = runTest {
        communityRepository.saveCommunities(listOf(createCommunity("1", "C1")), "service1")
        communityRepository.saveCommunities(listOf(createCommunity("2", "C2")), "service2")

        communityRepository.deleteAll()

        val result1 = communityRepository.getCommunitiesByServiceOnce("service1")
        val result2 = communityRepository.getCommunitiesByServiceOnce("service2")

        assertTrue(result1.isEmpty())
        assertTrue(result2.isEmpty())
    }

    @Test
    fun getCommunitiesByServiceOnce_emptyService_returnsEmptyList() = runTest {
        val result = communityRepository.getCommunitiesByServiceOnce("nonexistent")
        assertTrue(result.isEmpty())
    }
}
