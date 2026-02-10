package com.feedflow.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.local.db.entity.UrlBookmarkEntity
import com.feedflow.data.model.ForumThread
import com.feedflow.data.repository.BookmarkRepository
import com.feedflow.domain.service.ForumService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val forumServices: Map<String, @JvmSuppressWildcards ForumService>
) : ViewModel() {

    private val _threadBookmarks = MutableStateFlow<List<Pair<ForumThread, String>>>(emptyList())
    val threadBookmarks: StateFlow<List<Pair<ForumThread, String>>> = _threadBookmarks.asStateFlow()

    private val _urlBookmarks = MutableStateFlow<List<UrlBookmarkEntity>>(emptyList())
    val urlBookmarks: StateFlow<List<UrlBookmarkEntity>> = _urlBookmarks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBookmarks()
    }

    fun loadBookmarks() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                _threadBookmarks.value = bookmarkRepository.getAllBookmarksOnce()
                _urlBookmarks.value = bookmarkRepository.getAllUrlBookmarksOnce()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeThreadBookmark(thread: ForumThread, serviceId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(thread.id, serviceId)
            _threadBookmarks.value = _threadBookmarks.value.filter {
                it.first.id != thread.id || it.second != serviceId
            }
        }
    }

    fun removeUrlBookmark(url: String) {
        viewModelScope.launch {
            bookmarkRepository.removeUrlBookmark(url)
            _urlBookmarks.value = _urlBookmarks.value.filter { it.url != url }
        }
    }

    fun getService(serviceId: String): ForumService? = forumServices[serviceId]
}
