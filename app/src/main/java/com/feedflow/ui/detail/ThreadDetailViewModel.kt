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

sealed class ThreadDetailState {
    data object Loading : ThreadDetailState()
    data class Loaded(
        val thread: ForumThread,
        val comments: List<Comment>,
        val isLoadingMore: Boolean = false,
        val isFresh: Boolean = true,
        val isBookmarked: Boolean = false,
        val replyingTo: Comment? = null,
        val summary: String? = null,
        val isSummaryLoading: Boolean = false,
        val isSummaryCached: Boolean = false,
        val hasMorePages: Boolean = false
    ) : ThreadDetailState()
    data class Error(val message: String, val cachedThread: ForumThread? = null) : ThreadDetailState()
}

@HiltViewModel
class ThreadDetailViewModel @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val geminiService: GeminiService,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _state = MutableStateFlow<ThreadDetailState>(ThreadDetailState.Loading)
    val state: StateFlow<ThreadDetailState> = _state.asStateFlow()

    private var currentService: ForumService? = null
    private var currentThreadId: String? = null
    private var currentPage = 1
    private var totalPages: Int? = null

    fun loadThread(threadId: String, serviceId: String) {
        currentThreadId = threadId
        currentService = forumServices[serviceId]
        currentPage = 1

        viewModelScope.launch {
            _state.value = ThreadDetailState.Loading

            try {
                val cached = cacheRepository.getCachedThread(threadId)
                if (cached != null) {
                    _state.value = ThreadDetailState.Loaded(
                        thread = cached.first,
                        comments = cached.second,
                        isFresh = false
                    )
                }

                val service = currentService ?: throw Exception("Service not found")
                val result = service.fetchThreadDetail(threadId, 1)

                totalPages = result.totalPages
                val hasMorePages = totalPages != null && totalPages!! > 1

                cacheRepository.saveCachedThread(threadId, result.thread, result.comments)
                val isBookmarked = bookmarkRepository.isBookmarked(threadId, serviceId)

                _state.value = ThreadDetailState.Loaded(
                    thread = result.thread,
                    comments = result.comments,
                    isFresh = true,
                    isBookmarked = isBookmarked,
                    hasMorePages = hasMorePages
                )
            } catch (e: Exception) {
                val currentState = _state.value
                val cachedThread = if (currentState is ThreadDetailState.Loaded) currentState.thread else null
                _state.value = ThreadDetailState.Error(
                    message = e.message ?: "Failed to load thread",
                    cachedThread = cachedThread
                )
            }
        }
    }

    fun loadMoreComments() {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return
        if (currentState.isLoadingMore) return
        if (totalPages != null && currentPage >= totalPages!!) return

        viewModelScope.launch {
            _state.value = currentState.copy(isLoadingMore = true)

            try {
                val service = currentService ?: return@launch
                val threadId = currentThreadId ?: return@launch

                currentPage++
                val result = service.fetchThreadDetail(threadId, currentPage)
                
                if (result.totalPages != null) {
                    totalPages = result.totalPages
                }

                if (result.comments.isEmpty()) {
                    totalPages = currentPage
                }

                _state.value = currentState.copy(
                    comments = currentState.comments + result.comments,
                    isLoadingMore = false,
                    hasMorePages = totalPages != null && currentPage < totalPages!!
                )
            } catch (e: Exception) {
                currentPage--
                _state.value = currentState.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleBookmark() {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return

        viewModelScope.launch {
            val service = currentService ?: return@launch
            bookmarkRepository.toggleBookmark(currentState.thread, service.id)
            _state.value = currentState.copy(isBookmarked = !currentState.isBookmarked)
        }
    }

    fun selectReplyTarget(comment: Comment?) {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return
        _state.value = currentState.copy(replyingTo = comment)
    }

    fun sendReply(content: String) {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return

        viewModelScope.launch {
            try {
                val service = currentService ?: throw Exception("Service not found")
                val categoryId = currentState.thread.community.id

                val formattedContent = currentState.replyingTo?.let { reply ->
                    "[quote][b]${reply.author.username}:[/b]${reply.content.take(100)}...[/quote]\n$content"
                } ?: content

                service.postComment(currentState.thread.id, categoryId, formattedContent)
                _state.value = currentState.copy(replyingTo = null)
                loadThread(currentState.thread.id, service.id)
            } catch (e: Exception) {
                _state.value = ThreadDetailState.Error(e.message ?: "Failed to post reply")
            }
        }
    }

    fun generateSummary(forceRefresh: Boolean = false) {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return

        viewModelScope.launch {
            _state.value = currentState.copy(isSummaryLoading = true)

            try {
                val thread = currentState.thread

                if (!forceRefresh) {
                    val cached = cacheRepository.getSummaryIfFresh(thread.id, 7 * 24 * 60 * 60)
                    if (cached != null) {
                        _state.value = currentState.copy(
                            summary = cached,
                            isSummaryCached = true,
                            isSummaryLoading = false
                        )
                        return@launch
                    }
                }

                val commentsText = currentState.comments.take(10).joinToString("\n\n") {
                    "${it.author.username}: ${it.content}"
                }
                val fullContent = "${thread.title}\n\n${thread.content}\n\n$commentsText"

                val newSummary = geminiService.generateSummary(fullContent)
                cacheRepository.saveSummary(thread.id, newSummary)
                
                _state.value = currentState.copy(
                    summary = newSummary,
                    isSummaryCached = false,
                    isSummaryLoading = false
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(isSummaryLoading = false)
            }
        }
    }

    fun getWebURL(): String? {
        val currentState = _state.value
        if (currentState !is ThreadDetailState.Loaded) return null
        return currentService?.getWebURL(currentState.thread)
    }

    fun getService(): ForumService? = currentService
}
