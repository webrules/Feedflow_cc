package com.feedflow.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InAppBrowserViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private var currentUrl: String = ""
    private var currentTitle: String = ""

    fun init(url: String) {
        currentUrl = url
        checkBookmarkStatus()
    }

    fun updatePageInfo(url: String, title: String) {
        currentUrl = url
        currentTitle = title
        checkBookmarkStatus()
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            _isBookmarked.value = bookmarkRepository.isUrlBookmarked(currentUrl)
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            bookmarkRepository.toggleUrlBookmark(currentUrl, currentTitle)
            _isBookmarked.value = !_isBookmarked.value
        }
    }
}
