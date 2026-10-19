package com.example.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val items: List<Item>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { raw ->
            val q = raw.trim()
            flow {
                if (q.isBlank()) {
                    emit(SearchUiState.Idle)
                } else {
                    emit(SearchUiState.Loading)
                    runCatchingCancellable { itemRepository.searchByName(q) }
                        .onSuccess { emit(SearchUiState.Success(it)) }
                        .onFailure {
                            emit(
                                SearchUiState.Error(
                                    it.message ?: "Не удалось выполнить поиск"
                                )
                            )
                        }
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SearchUiState.Idle
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }
}