package com.example.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.UserCollection
import javax.inject.Inject

sealed interface CollectionPickerUiState {
    data object Loading : CollectionPickerUiState
    data class Success(val items: List<Item>) : CollectionPickerUiState
    data class Error(val message: String) : CollectionPickerUiState
}

@HiltViewModel
class CollectionPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<CollectionPickerUiState>(CollectionPickerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentCollection: UserCollection? = null

    init {
        viewModelScope.launch {
            collectionRepository.observeCollection(collectionId).collect { c ->
                currentCollection = c
                refresh()
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        refresh()
    }

    fun addItem(itemId: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            collectionRepository.addItem(collectionId, itemId)
                .onSuccess { onAdded() }
                .onFailure {
                    _uiState.value = CollectionPickerUiState.Error(
                        it.message ?: "Не удалось добавить"
                    )
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val q = _query.value.trim()
            val result = runCatching {
                if (q.isBlank()) itemRepository.getTopRated()
                else itemRepository.searchByName(q)
            }
            result
                .onSuccess { all ->
                    val excluded = currentCollection?.itemIds ?: emptyList()
                    val filtered = all.filterNot { it.id in excluded }
                    _uiState.value = CollectionPickerUiState.Success(filtered)
                }
                .onFailure {
                    _uiState.value = CollectionPickerUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}