package com.feedflow.ui.cover

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedflow.data.repository.CoverPageData
import com.feedflow.data.repository.CoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CoverViewModel"

sealed class CoverUiState {
    object Loading : CoverUiState()
    data class Success(val data: CoverPageData) : CoverUiState()
    data class Error(val message: String) : CoverUiState()
}

@HiltViewModel
class CoverViewModel @Inject constructor(
    private val coverRepository: CoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CoverUiState>(CoverUiState.Loading)
    val uiState: StateFlow<CoverUiState> = _uiState.asStateFlow()

    init {
        loadCover()
    }

    fun loadCover(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!forceRefresh) {
                    val cached = try {
                        coverRepository.getCoverPage(false)
                    } catch (e: Exception) {
                        null
                    }
                    if (cached != null && cached.fromCache) {
                        _uiState.value = CoverUiState.Success(cached)
                        refreshInBackground(cached)
                        return@launch
                    }
                }
                _uiState.value = CoverUiState.Loading
                val data = coverRepository.getCoverPage(forceRefresh)
                _uiState.value = CoverUiState.Success(data)
            } catch (e: Exception) {
                Log.e(TAG, "loadCover failed", e)
                _uiState.value = CoverUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun refreshInBackground(cachedData: CoverPageData) {
        val ageMs = System.currentTimeMillis() - cachedData.createdAt
        val eightHoursMs = 8 * 60 * 60 * 1000L
        if (ageMs < eightHoursMs) {
            Log.d(TAG, "Cache is ${ageMs / 1000}s old (< 8h), skipping background refresh")
            return
        }
        viewModelScope.launch {
            try {
                val freshData = coverRepository.generateFreshCover()
                _uiState.value = CoverUiState.Success(freshData)
            } catch (e: Exception) {
                Log.d(TAG, "Background refresh failed, keeping cached data", e)
            }
        }
    }
}
