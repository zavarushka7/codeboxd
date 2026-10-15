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

sealed interface LowestRatedUiState {
    data object Loading : LowestRatedUiState
    data class Success(val items: List<Item>) : LowestRatedUiState
    data class Error(val message: String) : LowestRatedUiState
}

@HiltViewModel
class LowestRatedViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LowestRatedUiState>(LowestRatedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { itemRepository.getLowestRated() }
                .onSuccess { _uiState.value = LowestRatedUiState.Success(it) }
                .onFailure {
                    _uiState.value = LowestRatedUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}