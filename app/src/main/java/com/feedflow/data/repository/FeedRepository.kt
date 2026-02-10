package com.feedflow.data.repository

import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.DefaultFeeds
import com.feedflow.data.model.FeedInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    val feeds: Flow<List<FeedInfo>> = preferencesManager.customRssFeeds.map { customFeedsJson ->
        val customFeeds = if (!customFeedsJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<FeedInfo>>(customFeedsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        DefaultFeeds.feeds + customFeeds
    }

    suspend fun getAllFeeds(): List<FeedInfo> {
        return feeds.first()
    }

    suspend fun getCustomFeeds(): List<FeedInfo> {
        val allFeeds = getAllFeeds()
        val defaultIds = DefaultFeeds.feeds.map { it.id }.toSet()
        return allFeeds.filter { it.id !in defaultIds }
    }

    suspend fun addFeed(name: String, url: String, description: String = "") {
        val currentCustom = getCustomFeeds()
        val newFeed = FeedInfo(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            url = url,
            description = description
        )
        val updated = currentCustom + newFeed
        preferencesManager.setCustomRssFeeds(json.encodeToString(updated))
    }

    suspend fun removeFeed(feedId: String) {
        val currentCustom = getCustomFeeds()
        val updated = currentCustom.filter { it.id != feedId }
        preferencesManager.setCustomRssFeeds(json.encodeToString(updated))
    }

    suspend fun removeFeeds(ids: Set<String>) {
        val currentCustom = getCustomFeeds()
        val updated = currentCustom.filter { it.id !in ids }
        preferencesManager.setCustomRssFeeds(json.encodeToString(updated))
    }

    fun isDefaultFeed(feedId: String): Boolean {
        return DefaultFeeds.feeds.any { it.id == feedId }
    }
}
