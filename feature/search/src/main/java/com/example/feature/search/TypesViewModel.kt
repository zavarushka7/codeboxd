package ru.savushkina.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ItemRepository
import javax.inject.Inject

sealed interface TypesUiState {
    data object Loading : TypesUiState
    data class Success(val types: List<String>) : TypesUiState
    data class Error(val message: String) : TypesUiState
}

@HiltViewModel
class TypesViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TypesUiState>(TypesUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { itemRepository.getTypes() }
                .onSuccess { _uiState.value = TypesUiState.Success(it) }
                .onFailure {
                    _uiState.value = TypesUiState.Error(
                        it.message ?: "Не удалось загрузить типы"
                    )
                }
        }
    }
}