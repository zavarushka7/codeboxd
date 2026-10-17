package ru.savushkina.feature.collection

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
import javax.inject.Inject

sealed interface CollectionDetailUiState {
    data object Loading : CollectionDetailUiState
    data class Success(
        val collection: UserCollection,
        val items: List<Item>
    ) : CollectionDetailUiState
    data class Error(val message: String) : CollectionDetailUiState
}

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])

    val currentUserId: String?
        get() = authRepository.currentUserId

    private val _uiState = MutableStateFlow<CollectionDetailUiState>(CollectionDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            collectionRepository.observeCollection(collectionId).collect { collection ->
                if (collection == null) {
                    _uiState.value = CollectionDetailUiState.Error("Коллекция не найдена")
                } else {
                    val items = collection.itemIds.mapNotNull { id ->
                        itemRepository.getItemById(id)
                    }
                    _uiState.value = CollectionDetailUiState.Success(collection, items)
                }
            }
        }
    }

    val isOwnCollection: Boolean
        get() {
            val current = _uiState.value
            return current is CollectionDetailUiState.Success &&
                    current.collection.userId == currentUserId
        }

    fun removeItem(itemId: String) {
        if (!isOwnCollection) return
        viewModelScope.launch {
            collectionRepository.removeItem(collectionId, itemId)
        }
    }

    fun deleteCollection(onDeleted: () -> Unit) {
        if (!isOwnCollection) return
        viewModelScope.launch {
            collectionRepository.deleteCollection(collectionId)
                .onSuccess { onDeleted() }
        }
    }

    fun rename(newName: String) {
        if (!isOwnCollection) return
        viewModelScope.launch {
            collectionRepository.rename(collectionId, newName)
        }
    }
}