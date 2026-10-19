package com.example.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface TopRatedUiState {
    data object Loading : TopRatedUiState
    data class Success(val items: List<Item>) : TopRatedUiState
    data class Error(val message: String) : TopRatedUiState
}

@HiltViewModel
class TopRatedViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TopRatedUiState>(TopRatedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatchingCancellable { itemRepository.getTopRated() }
                .onSuccess { _uiState.value = TopRatedUiState.Success(it) }
                .onFailure {
                    _uiState.value = TopRatedUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}