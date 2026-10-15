package ru.savushkina.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val items: List<Item>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            itemRepository.observeItems().collect { items ->
                _uiState.value = FeedUiState.Success(items)
            }
        }
    }
}