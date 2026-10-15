package ru.savushkina.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface TypeItemsUiState {
    data object Loading : TypeItemsUiState
    data class Success(val items: List<Item>) : TypeItemsUiState
    data class Error(val message: String) : TypeItemsUiState
}

@HiltViewModel
class TypeItemsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val type: String = java.net.URLDecoder.decode(
        checkNotNull(savedStateHandle["type"]),
        "UTF-8"
    )

    val typeName: String get() = type

    private val _uiState = MutableStateFlow<TypeItemsUiState>(TypeItemsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { itemRepository.getItemsByType(type) }
                .onSuccess { _uiState.value = TypeItemsUiState.Success(it) }
                .onFailure {
                    _uiState.value = TypeItemsUiState.Error(
                        it.message ?: "Не удалось загрузить айтемы"
                    )
                }
        }
    }
}