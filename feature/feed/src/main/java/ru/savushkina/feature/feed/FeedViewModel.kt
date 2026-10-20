package ru.savushkina.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(
        val items: List<Item>,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true,
        val loadMoreError: String? = null
    ) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var nextCursor: String? = null

    init {
        loadFirstPage(isRefresh = false)
    }

    /**
     * @param isRefresh true — pull-to-refresh: НЕ сбрасываем UI в Loading,
     *                  показываем индикатор PullToRefreshBox, данные обновятся
     *                  по завершении. false — обычная первая загрузка.
     */
    private fun loadFirstPage(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = FeedUiState.Loading
            }
            nextCursor = null
            try {
                itemRepository.loadItemsPage(PAGE_SIZE, null)
                    .onSuccess { page ->
                        nextCursor = page.nextCursor
                        _uiState.value = FeedUiState.Success(
                            items = page.items,
                            canLoadMore = page.nextCursor != null,
                            loadMoreError = null
                        )
                    }
                    .onFailure { e ->
                        // При refresh ошибку не показываем как полноэкранную —
                        // оставляем старые данные, ошибка видна по индикатору.
                        if (!isRefresh) {
                            _uiState.value = FeedUiState.Error(
                                e.message ?: "Не удалось загрузить ленту"
                            )
                        }
                    }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        loadFirstPage(isRefresh = true)
    }

    fun loadMore() {
        val current = _uiState.value
        if (current !is FeedUiState.Success) return
        if (current.isLoadingMore || !current.canLoadMore) return
        if (current.loadMoreError != null) return
        val cursor = nextCursor ?: return

        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)

        viewModelScope.launch {
            itemRepository.loadItemsPage(PAGE_SIZE, cursor)
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _uiState.update { state ->
                        if (state is FeedUiState.Success) {
                            state.copy(
                                items = state.items + page.items,
                                isLoadingMore = false,
                                canLoadMore = page.nextCursor != null,
                                loadMoreError = null
                            )
                        } else state
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is FeedUiState.Success) {
                            state.copy(
                                isLoadingMore = false,
                                loadMoreError = e.message ?: "Не удалось загрузить ещё"
                            )
                        } else state
                    }
                }
        }
    }

    fun retryLoadMore() {
        val current = _uiState.value as? FeedUiState.Success ?: return
        if (current.loadMoreError == null) return
        _uiState.value = current.copy(loadMoreError = null)
        loadMore()
    }

    fun retry() {
        loadFirstPage(isRefresh = false)
    }

    private companion object {
        const val PAGE_SIZE = 20L
    }
}