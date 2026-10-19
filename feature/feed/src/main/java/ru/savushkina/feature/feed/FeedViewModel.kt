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
        /** Ошибка последней попытки догрузки. Не влияет на уже загруженные items. */
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

    private var nextCursor: String? = null

    init {
        loadFirstPage()
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            nextCursor = null
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
                    _uiState.value = FeedUiState.Error(
                        e.message ?: "Не удалось загрузить ленту"
                    )
                }
        }
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
                    // Уже загруженные элементы сохраняем. Показываем ошибку
                    // под списком с кнопкой «повторить».
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

    /** Повторить именно догрузку (после ошибки), не сбрасывая список. */
    fun retryLoadMore() {
        val current = _uiState.value as? FeedUiState.Success ?: return
        if (current.loadMoreError == null) return
        _uiState.value = current.copy(loadMoreError = null)
        loadMore()
    }

    fun retry() {
        loadFirstPage()
    }

    private companion object {
        const val PAGE_SIZE = 20L
    }
}