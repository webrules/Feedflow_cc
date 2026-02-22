package com.feedflow.ui.threads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.repository.CacheRepository
import com.feedflow.data.repository.CommunityRepository
import com.feedflow.data.repository.UserRepository
import com.feedflow.domain.service.ForumService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ThreadListViewModel @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ForumThread>>(emptyList())
    val threads: StateFlow<List<ForumThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _communityName = MutableStateFlow<String?>(null)
    val communityName: StateFlow<String?> = _communityName.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var currentService: ForumService? = null
    private var currentCommunity: Community? = null
    private var communities: List<Community> = emptyList()
    private var currentPage = 1
    private var currentServiceId: String? = null
    private val prefetchJobs = mutableMapOf<String, Job>()

    fun requiresLogin(): Boolean = currentService?.requiresLogin() ?: false

    fun loadThreadsBySiteAndCommunity(serviceId: String, communityId: String) {
        if (currentServiceId == serviceId && currentCommunity?.id == communityId && _threads.value.isNotEmpty()) {
            return // Already loaded
        }
        currentServiceId = serviceId
        currentService = forumServices[serviceId]

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _canLoadMore.value = true
            _isLoggedIn.value = userRepository.isLoggedIn(serviceId)
            currentPage = 1

            try {
                val service = currentService ?: throw Exception("Service not found")

                // Load communities from cache or fetch them
                communities = communityRepository.getCommunitiesByServiceOnce(serviceId)
                if (communities.isEmpty()) {
                    communities = service.fetchCategories()
                    communityRepository.saveCommunities(communities, serviceId)
                }

                val community = communities.find { it.id == communityId }
                    ?: Community(communityId, communityId, "", serviceId)
                currentCommunity = community
                _communityName.value = community.name

                val cacheKey = "${serviceId}_${communityId}_1"

                // Load from cache first for instant display
                val cached = cacheRepository.getCachedTopics(cacheKey)
                if (cached != null) {
                    _threads.value = cached
                    _isLoading.value = false
                }

                // Fetch fresh data
                val freshThreads = service.fetchCategoryThreads(communityId, communities, 1)

                if (freshThreads.isEmpty() && _threads.value.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    _threads.value = freshThreads
                    cacheRepository.saveCachedTopics(cacheKey, freshThreads)
                }
            } catch (e: Exception) {
                android.util.Log.e("ThreadListVM", "Error loading threads: ${e.javaClass.simpleName}: ${e.message}", e)
                val errorMessage = when {
                    e is HttpException && e.code() == 403 -> "Cloudflare protection (403) - Tap verify button"
                    e is HttpException -> "HTTP ${e.code()}: ${e.message()}"
                    e.message?.contains("403") == true -> "Cloudflare protection (403) - Tap verify button"
                    e.message?.contains("HTTP 403") == true -> "Cloudflare protection (403) - Tap verify button"
                    else -> e.message ?: "Failed to load threads"
                }
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadThreads(
        serviceId: String,
        community: Community,
        allCommunities: List<Community>,
        isReturning: Boolean = false
    ) {
        currentService = forumServices[serviceId]
        currentCommunity = community
        currentServiceId = serviceId
        communities = allCommunities
        _communityName.value = community.name
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _canLoadMore.value = true

            try {
                val cacheKey = "${serviceId}_${community.id}_1"

                // Load from cache first for instant display
                if (isReturning) {
                    val cached = cacheRepository.getCachedTopics(cacheKey)
                    if (cached != null) {
                        _threads.value = cached
                        _isLoading.value = false
                    }
                }

                // Fetch fresh data
                val service = currentService ?: throw Exception("Service not found")
                val freshThreads = service.fetchCategoryThreads(community.id, communities, 1)

                if (freshThreads.isEmpty() && _threads.value.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    _threads.value = freshThreads
                    cacheRepository.saveCachedTopics(cacheKey, freshThreads)
                }
            } catch (e: Exception) {
                if (_threads.value.isEmpty()) {
                    _error.value = e.message ?: "Failed to load threads"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_canLoadMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val service = currentService ?: return@launch
                val community = currentCommunity ?: return@launch

                currentPage++
                val moreThreads = service.fetchCategoryThreads(community.id, communities, currentPage)

                if (moreThreads.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    _threads.value = _threads.value + moreThreads
                }
            } catch (e: Exception) {
                currentPage--
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun prefetchThread(thread: ForumThread) {
        // Skip if already cached
        viewModelScope.launch {
            if (cacheRepository.hasCache(thread.id)) return@launch

            // Cancel existing prefetch for this thread
            prefetchJobs[thread.id]?.cancel()

            // Debounced prefetch
            prefetchJobs[thread.id] = viewModelScope.launch {
                delay(400)

                try {
                    val service = currentService ?: return@launch
                    val result = service.fetchThreadDetail(thread.id, 1)
                    cacheRepository.saveCachedThread(thread.id, result.thread, result.comments)
                } catch (e: Exception) {
                    // Silent failure for prefetch
                }
            }
        }
    }

    fun cancelPrefetch(threadId: String) {
        prefetchJobs[threadId]?.cancel()
        prefetchJobs.remove(threadId)
    }

    fun removeThread(thread: ForumThread) {
        _threads.value = _threads.value.filter { it.id != thread.id }
    }

    fun refresh() {
        currentCommunity?.let { community ->
            currentService?.let { service ->
                loadThreads(service.id, community, communities, false)
            }
        }
    }

    fun refreshLoginStatus() {
        currentServiceId?.let { serviceId ->
            _isLoggedIn.value = userRepository.isLoggedIn(serviceId)
        }
    }

    fun getService(): ForumService? = currentService

    fun getThreads(): List<ForumThread> = _threads.value
}
