package com.feedflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val allOptionalIds = listOf(
        ForumSite.HACKER_NEWS,
        ForumSite.FOUR_D4Y,
        ForumSite.V2EX,
        ForumSite.LINUX_DO,
        ForumSite.ZHIHU
    ).map { it.id }.toSet()

    val sites: StateFlow<List<ForumSite>> = preferencesManager.communityVisibility
        .map { csv ->
            val enabledIds = if (csv.isNullOrEmpty()) allOptionalIds
            else csv.split(",").filter { it.isNotBlank() }.toSet()
            ForumSite.entries.filter { it == ForumSite.RSS || enabledIds.contains(it.id) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForumSite.entries.toList())

    private val _loginStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loginStatus: StateFlow<Map<String, Boolean>> = _loginStatus.asStateFlow()

    init {
        refreshLoginStatus()
    }

    fun refreshLoginStatus() {
        viewModelScope.launch {
            _loginStatus.value = userRepository.getLoginStatusMap()
        }
    }

    fun isLoggedIn(siteId: String): Boolean {
        return _loginStatus.value[siteId] == true
    }
}
