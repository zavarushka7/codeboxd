package com.example.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.UserCollection
import java.net.URLDecoder
import javax.inject.Inject

sealed interface ArrayPickerUiState {
    data object Loading : ArrayPickerUiState
    data class Success(val items: List<Item>) : ArrayPickerUiState
    data class Error(val message: String) : ArrayPickerUiState
}

@HiltViewModel
class ArrayPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val collectionRepository: CollectionRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val name: String = URLDecoder.decode(
        savedStateHandle.get<String>("name") ?: "",
        "UTF-8"
    )
    private val allowedType: String = URLDecoder.decode(
        savedStateHandle.get<String>("type") ?: "",
        "UTF-8"
    )
    private val userId: String = authRepository.currentUserId
        ?: throw IllegalStateException("Не авторизован")

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds = _selectedIds.asStateFlow()

    private val _uiState = MutableStateFlow<ArrayPickerUiState>(ArrayPickerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val typeName: String get() = allowedType

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        _query.value = value
        refresh()
    }

    fun toggleItem(itemId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            if (current.size >= UserCollection.MAX_SIZE_LIMIT) current
            else current + itemId
        }
    }

    fun submit(onSuccess: (String) -> Unit) {
        val selected = _selectedIds.value
        if (selected.size < UserCollection.MIN_SIZE) {
            _error.value = "Выберите минимум ${UserCollection.MIN_SIZE} айтема"
            return
        }
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            val collection = UserCollection(
                userId = userId,
                name = name,
                itemIds = selected,
                isFixedSize = true,
                maxSize = selected.size,     // размер = число выбранных
                allowedItemType = allowedType
            )
            collectionRepository.createCollection(collection)
                .onSuccess {
                    _isSaving.value = false
                    onSuccess(it)
                }
                .onFailure {
                    _isSaving.value = false
                    _error.value = it.message ?: "Не удалось создать массив"
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val q = _query.value.trim()
            _uiState.value = ArrayPickerUiState.Loading
            val result = runCatching {
                if (q.isBlank()) {
                    itemRepository.searchByType(allowedType)
                } else {
                    itemRepository.searchByName(q).filter { it.type == allowedType }
                }
            }
            result
                .onSuccess { _uiState.value = ArrayPickerUiState.Success(it) }
                .onFailure {
                    _uiState.value = ArrayPickerUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}