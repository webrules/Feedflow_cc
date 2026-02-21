package com.feedflow.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.feedflow.data.local.encryption.EncryptionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "feedflow_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionHelper: EncryptionHelper
) {
    private val dataStore = context.dataStore

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "en"
    }

    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    val geminiApiKey: Flow<String?> = dataStore.data.map { 
        encryptionHelper.getCredential(EncryptionHelper.KEY_GEMINI_API_KEY)
    }

    suspend fun setGeminiApiKey(apiKey: String?) {
        if (apiKey != null) {
            encryptionHelper.saveCredential(EncryptionHelper.KEY_GEMINI_API_KEY, apiKey)
        } else {
            encryptionHelper.removeCredential(EncryptionHelper.KEY_GEMINI_API_KEY)
        }
    }

    val customRssFeeds: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CUSTOM_RSS_FEEDS_KEY]
    }

    suspend fun setCustomRssFeeds(feedsJson: String) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_RSS_FEEDS_KEY] = feedsJson
        }
    }

    val downvotedIds: Flow<String?> = dataStore.data.map { preferences ->
        preferences[DOWNVOTED_IDS_KEY]
    }

    suspend fun setDownvotedIds(ids: String) {
        dataStore.edit { preferences ->
            preferences[DOWNVOTED_IDS_KEY] = ids
        }
    }

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
        private val CUSTOM_RSS_FEEDS_KEY = stringPreferencesKey("custom_rss_feeds")
        private val DOWNVOTED_IDS_KEY = stringPreferencesKey("downvoted_ids")
        private val COMMUNITY_VISIBILITY_KEY = stringPreferencesKey("community_visibility")
    }
}
