package com.example.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import javax.inject.Inject

sealed interface CollectionTypePickerUiState {
    data object Loading : CollectionTypePickerUiState
    data class Success(val types: List<String>) : CollectionTypePickerUiState
    data class Error(val message: String) : CollectionTypePickerUiState
}

@HiltViewModel
class CollectionTypePickerViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectionTypePickerUiState>(
        CollectionTypePickerUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { itemRepository.getTypes() }
                .onSuccess { _uiState.value = CollectionTypePickerUiState.Success(it) }
                .onFailure {
                    _uiState.value = CollectionTypePickerUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}