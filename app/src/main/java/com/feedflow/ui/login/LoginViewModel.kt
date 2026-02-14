package com.feedflow.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loginStatus: StateFlow<Map<String, Boolean>> = _loginStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val loginableSites: List<ForumSite> = ForumSite.entries.filter {
        it != ForumSite.RSS
    }

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

    fun saveCookies(siteId: String, cookiesJson: String) {
        viewModelScope.launch {
            try {
                userRepository.saveCookies(siteId, cookiesJson)
                refreshLoginStatus()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save login data"
            }
        }
    }

    fun logout(siteId: String) {
        viewModelScope.launch {
            try {
                userRepository.logout(siteId)
                refreshLoginStatus()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to logout"
            }
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            try {
                userRepository.logoutAll()
                refreshLoginStatus()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to logout"
            }
        }
    }

    fun getLoginUrl(site: ForumSite): String {
        return when (site) {
            ForumSite.HACKER_NEWS -> "https://news.ycombinator.com/login"
            ForumSite.V2EX -> "${site.baseUrl}/signin"
            ForumSite.LINUX_DO -> "${site.baseUrl}/login"
            ForumSite.FOUR_D4Y -> "${site.baseUrl}/logging.php?action=login"
            ForumSite.ZHIHU -> "${site.baseUrl}/signin"
            ForumSite.NODE_SEEK -> "${site.baseUrl}/signIn.html"
            ForumSite.TWO_LIBRA -> "${site.baseUrl}/auth/login"
            else -> site.baseUrl
        }
    }

    fun clearError() {
        _error.value = null
    }
}
