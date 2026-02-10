package com.feedflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.ForumSite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private val OPTIONAL_SITES = listOf(
            ForumSite.HACKER_NEWS,
            ForumSite.FOUR_D4Y,
            ForumSite.V2EX,
            ForumSite.LINUX_DO,
            ForumSite.ZHIHU
        )
        private val ALL_OPTIONAL_IDS = OPTIONAL_SITES.map { it.id }.toSet()
    }

    val isDarkMode: StateFlow<Boolean> = preferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = preferencesManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val geminiApiKey: StateFlow<String?> = preferencesManager.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val enabledSites: StateFlow<Set<String>> = preferencesManager.communityVisibility
        .map { csv ->
            if (csv.isNullOrEmpty()) ALL_OPTIONAL_IDS
            else csv.split(",").filter { it.isNotBlank() }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ALL_OPTIONAL_IDS)

    val optionalSites: List<ForumSite> = OPTIONAL_SITES

    fun toggleSite(siteId: String) {
        viewModelScope.launch {
            val current = enabledSites.value.toMutableSet()
            if (current.contains(siteId)) {
                current.remove(siteId)
            } else {
                current.add(siteId)
            }
            preferencesManager.setCommunityVisibility(current.joinToString(","))
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            preferencesManager.setDarkMode(!isDarkMode.value)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val newLanguage = if (language.value == "en") "zh" else "en"
            preferencesManager.setLanguage(newLanguage)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(languageCode)
        }
    }

    fun setGeminiApiKey(apiKey: String?) {
        viewModelScope.launch {
            preferencesManager.setGeminiApiKey(apiKey)
        }
    }
}
