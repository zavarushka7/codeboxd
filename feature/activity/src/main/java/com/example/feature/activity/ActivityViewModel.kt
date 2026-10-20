package ru.savushkina.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.FeedRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.FeedItem
import ru.savushkina.core.model.SessionState
import javax.inject.Inject

sealed interface ActivityUiState {
    data object Loading : ActivityUiState
    data class Success(
        val items: List<FeedItem>,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true,
        val loadMoreError: String? = null
    ) : ActivityUiState
    data class Error(val message: String) : ActivityUiState
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val currentUserId: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                is SessionState.Authenticated -> state.user.uid
                is SessionState.Onboarding -> state.userId
                is SessionState.ProfileLoadError -> state.userId
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var nextCursor: Long? = null

    init {
        viewModelScope.launch {
            currentUserId
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(ActivityUiState.Error("Не авторизован"))
                    } else {
                        nextCursor = null
                        feedRepository.observeFeed(uid, FeedRepository.DEFAULT_PAGE_SIZE)
                            .map { items ->
                                nextCursor = items.lastOrNull()?.timestampMillis
                                ActivityUiState.Success(
                                    items = items,
                                    canLoadMore = items.size >= FeedRepository.DEFAULT_PAGE_SIZE
                                ) as ActivityUiState
                            }
                            .catch { emit(ActivityUiState.Error(it.message ?: "Ошибка загрузки")) }
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current !is ActivityUiState.Success) return
        if (current.isLoadingMore || !current.canLoadMore) return
        if (current.loadMoreError != null) return
        val uid = currentUserId.value ?: return
        val cursor = nextCursor ?: return

        _uiState.update { state ->
            if (state is ActivityUiState.Success) {
                state.copy(isLoadingMore = true, loadMoreError = null)
            } else {
                state
            }
        }

        viewModelScope.launch {
            feedRepository.loadMore(uid, cursor, FeedRepository.DEFAULT_PAGE_SIZE)
                .onSuccess { page ->
                    nextCursor = page.lastOrNull()?.timestampMillis
                    _uiState.update { state ->
                        if (state is ActivityUiState.Success) {
                            state.copy(
                                items = state.items + page,
                                isLoadingMore = false,
                                canLoadMore = page.size >= FeedRepository.DEFAULT_PAGE_SIZE,
                                loadMoreError = null
                            )
                        } else {
                            state
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is ActivityUiState.Success) {
                            state.copy(
                                isLoadingMore = false,
                                loadMoreError = e.message ?: "Не удалось загрузить ещё"
                            )
                        } else {
                            state
                        }
                    }
                }
        }
    }

    fun retryLoadMore() {
        val current = _uiState.value
        if (current !is ActivityUiState.Success) return
        if (current.loadMoreError == null) return
        _uiState.update { state ->
            if (state is ActivityUiState.Success) {
                state.copy(loadMoreError = null)
            } else {
                state
            }
        }
        loadMore()
    }
}