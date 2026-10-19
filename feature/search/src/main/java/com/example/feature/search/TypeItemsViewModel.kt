package com.example.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.navigation.Routes

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

    private val type: String = Routes.decode(savedStateHandle.get<String>("type"))
    val typeName: String get() = type

    private val _uiState = MutableStateFlow<TypeItemsUiState>(TypeItemsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        if (type.isBlank()) {
            _uiState.value = TypeItemsUiState.Error("Некорректный тип")
        } else {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            runCatchingCancellable { itemRepository.getItemsByType(type) }
                .onSuccess { _uiState.value = TypeItemsUiState.Success(it) }
                .onFailure {
                    _uiState.value = TypeItemsUiState.Error(
                        it.message ?: "Не удалось загрузить айтемы"
                    )
                }
        }
    }
}