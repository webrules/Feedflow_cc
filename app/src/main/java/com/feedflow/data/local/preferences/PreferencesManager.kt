package com.feedflow.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "feedflow_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Dark Mode
    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    // Language
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "en"
    }

    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    // Gemini API Key
    val geminiApiKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY]
    }

    suspend fun setGeminiApiKey(apiKey: String?) {
        dataStore.edit { preferences ->
            if (apiKey != null) {
                preferences[GEMINI_API_KEY] = apiKey
            } else {
                preferences.remove(GEMINI_API_KEY)
            }
        }
    }

    // Custom RSS Feeds (JSON encoded)
    val customRssFeeds: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CUSTOM_RSS_FEEDS_KEY]
    }

    suspend fun setCustomRssFeeds(feedsJson: String) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_RSS_FEEDS_KEY] = feedsJson
        }
    }

    // Downvoted IDs (for Zhihu)
    val downvotedIds: Flow<String?> = dataStore.data.map { preferences ->
        preferences[DOWNVOTED_IDS_KEY]
    }

    suspend fun setDownvotedIds(ids: String) {
        dataStore.edit { preferences ->
            preferences[DOWNVOTED_IDS_KEY] = ids
        }
    }

    // Community Visibility Settings
    val communityVisibility: Flow<String?> = dataStore.data.map { preferences ->
        preferences[COMMUNITY_VISIBILITY_KEY]
    }

    suspend fun setCommunityVisibility(settings: String) {
        dataStore.edit { preferences ->
            preferences[COMMUNITY_VISIBILITY_KEY] = settings
        }
    }

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val CUSTOM_RSS_FEEDS_KEY = stringPreferencesKey("custom_rss_feeds")
        private val DOWNVOTED_IDS_KEY = stringPreferencesKey("downvoted_ids")
        private val COMMUNITY_VISIBILITY_KEY = stringPreferencesKey("community_visibility")
    }
}
