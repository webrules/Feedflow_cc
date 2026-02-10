package com.feedflow.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumSite
import com.feedflow.data.repository.CommunityRepository
import com.feedflow.domain.service.ForumService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _communities = MutableStateFlow<List<Community>>(emptyList())
    val communities: StateFlow<List<Community>> = _communities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSite: ForumSite? = null
    private var currentService: ForumService? = null

    fun loadCommunities(site: ForumSite) {
        currentSite = site
        currentService = forumServices[site.id]

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

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
                if (_communities.value.isEmpty()) {
                    _error.value = e.message ?: "Failed to load communities"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        currentSite?.let { loadCommunities(it) }
    }

    fun getService(): ForumService? = currentService
}
