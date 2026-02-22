package com.feedflow.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.CommunityRepository
import com.feedflow.data.repository.UserRepository
import com.feedflow.domain.service.ForumService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _communities = MutableStateFlow<List<Community>>(emptyList())
    val communities: StateFlow<List<Community>> = _communities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var currentSite: ForumSite? = null
    private var currentService: ForumService? = null

    fun requiresLogin(): Boolean = currentService?.requiresLogin() ?: false

    fun loadCommunities(site: ForumSite) {
        currentSite = site
        currentService = forumServices[site.id]

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isLoggedIn.value = userRepository.isLoggedIn(site.id)

            try {
                // Try to load from cache first
                val cached = communityRepository.getCommunitiesByServiceOnce(site.id)
                if (cached.isNotEmpty()) {
                    _communities.value = cached
                }

                // Fetch fresh data
                val service = currentService ?: throw Exception("Service not found")
                val freshCommunities = service.fetchCategories()

                _communities.value = freshCommunities

                // Save to cache
                communityRepository.saveCommunities(freshCommunities, site.id)
            } catch (e: Exception) {
                android.util.Log.e("CommunitiesVM", "Error loading communities: ${e.javaClass.simpleName}: ${e.message}", e)
                if (_communities.value.isEmpty()) {
                    val errorMessage = when {
                        e is HttpException && e.code() == 403 -> "Cloudflare protection (403) - Tap verify button"
                        e is HttpException -> "HTTP ${e.code()}: ${e.message()}"
                        e.message?.contains("403") == true -> "Cloudflare protection (403) - Tap verify button"
                        e.message?.contains("HTTP 403") == true -> "Cloudflare protection (403) - Tap verify button"
                        else -> e.message ?: "Failed to load communities"
                    }
                    _error.value = errorMessage
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        currentSite?.let { loadCommunities(it) }
    }

    fun refreshLoginStatus() {
        currentSite?.let { site ->
            _isLoggedIn.value = userRepository.isLoggedIn(site.id)
        }
    }

    fun getService(): ForumService? = currentService
}
