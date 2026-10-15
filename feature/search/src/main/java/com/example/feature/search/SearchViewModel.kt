package ru.savushkina.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val items: List<Item>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
        if (value.isBlank()) {
            _uiState.value = SearchUiState.Idle
        } else {
            searchByName(value)
        }
    }

    private fun searchByName(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            runCatching { itemRepository.searchByName(query) }
                .onSuccess { _uiState.value = SearchUiState.Success(it) }
                .onFailure {
                    _uiState.value = SearchUiState.Error(
                        it.message ?: "Не удалось выполнить поиск"
                    )
                }
        }
    }
}