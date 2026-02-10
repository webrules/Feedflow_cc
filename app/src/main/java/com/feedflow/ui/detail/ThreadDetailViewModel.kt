package com.feedflow.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.model.Comment
import com.feedflow.data.model.ForumThread
import com.feedflow.data.repository.BookmarkRepository
import com.feedflow.data.repository.CacheRepository
import com.feedflow.domain.service.ForumService
import com.feedflow.domain.service.GeminiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreadDetailViewModel @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val geminiService: GeminiService,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _thread = MutableStateFlow<ForumThread?>(null)
    val thread: StateFlow<ForumThread?> = _thread.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isFresh = MutableStateFlow(true)
    val isFresh: StateFlow<Boolean> = _isFresh.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _replyingTo = MutableStateFlow<Comment?>(null)
    val replyingTo: StateFlow<Comment?> = _replyingTo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // AI Summary
    private val _summary = MutableStateFlow<String?>(null)
    val summary: StateFlow<String?> = _summary.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    private val _isSummaryCached = MutableStateFlow(false)
    val isSummaryCached: StateFlow<Boolean> = _isSummaryCached.asStateFlow()

    private var currentService: ForumService? = null
    private var currentThreadId: String? = null
    private var currentPage = 1
    private var totalPages: Int? = null

    fun loadThread(threadId: String, serviceId: String) {
        currentThreadId = threadId
        currentService = forumServices[serviceId]
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isFresh.value = false

            try {
                // Load from cache first
                val cached = cacheRepository.getCachedThread(threadId)
                if (cached != null) {
                    _thread.value = cached.first
                    _comments.value = cached.second
                    _isLoading.value = false
                }

                // Fetch fresh data
                val service = currentService ?: throw Exception("Service not found")
                val result = service.fetchThreadDetail(threadId, 1)

                _thread.value = result.thread
                _comments.value = result.comments
                totalPages = result.totalPages
                _isFresh.value = true

                // Save to cache
                cacheRepository.saveCachedThread(threadId, result.thread, result.comments)

                // Check bookmark status
                _isBookmarked.value = bookmarkRepository.isBookmarked(threadId, serviceId)
            } catch (e: Exception) {
                if (_thread.value == null) {
                    _error.value = e.message ?: "Failed to load thread"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreComments() {
        if (_isLoadingMore.value) return
        if (totalPages != null && currentPage >= totalPages!!) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val service = currentService ?: return@launch
                val threadId = currentThreadId ?: return@launch

                currentPage++
                val result = service.fetchThreadDetail(threadId, currentPage)
                _comments.value = _comments.value + result.comments

                // Update totalPages from latest response
                if (result.totalPages != null) {
                    totalPages = result.totalPages
                }

                // Stop if no new comments were returned
                if (result.comments.isEmpty()) {
                    totalPages = currentPage
                }
            } catch (e: Exception) {
                currentPage--
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val thread = _thread.value ?: return@launch
            val service = currentService ?: return@launch

            bookmarkRepository.toggleBookmark(thread, service.id)
            _isBookmarked.value = !_isBookmarked.value
        }
    }

    fun selectReplyTarget(comment: Comment?) {
        _replyingTo.value = comment
    }

    fun sendReply(content: String) {
        viewModelScope.launch {
            try {
                val service = currentService ?: throw Exception("Service not found")
                val thread = _thread.value ?: throw Exception("Thread not found")
                val categoryId = thread.community.id

                // Format content with quote if replying
                val formattedContent = _replyingTo.value?.let { reply ->
                    "[quote][b]${reply.author.username}:[/b]${reply.content.take(100)}...[/quote]\n$content"
                } ?: content

                service.postComment(thread.id, categoryId, formattedContent)

                // Clear reply target
                _replyingTo.value = null

                // Refresh to show new comment
                loadThread(thread.id, service.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to post reply"
            }
        }
    }

    fun generateSummary(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isSummaryLoading.value = true
            _error.value = null

            try {
                val thread = _thread.value ?: throw Exception("No thread loaded")

                // Check cache first
                if (!forceRefresh) {
                    val cached = cacheRepository.getSummaryIfFresh(thread.id, 7 * 24 * 60 * 60) // 7 days
                    if (cached != null) {
                        _summary.value = cached
                        _isSummaryCached.value = true
                        _isSummaryLoading.value = false
                        return@launch
                    }
                }

                // Build content for summary
                val commentsText = _comments.value.take(10).joinToString("\n\n") {
                    "${it.author.username}: ${it.content}"
                }
                val fullContent = "${thread.title}\n\n${thread.content}\n\n$commentsText"

                // Generate new summary
                val newSummary = geminiService.generateSummary(fullContent)
                _summary.value = newSummary
                _isSummaryCached.value = false

                // Save to cache
                cacheRepository.saveSummary(thread.id, newSummary)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate summary"
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }

    fun getWebURL(): String? {
        val thread = _thread.value ?: return null
        val service = currentService ?: return null
        return service.getWebURL(thread)
    }

    fun getService(): ForumService? = currentService
}
