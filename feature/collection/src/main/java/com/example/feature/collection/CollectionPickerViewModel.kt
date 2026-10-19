package com.example.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.UserCollection
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface CollectionPickerUiState {
    data object Loading : CollectionPickerUiState
    data class Success(val items: List<Item>) : CollectionPickerUiState
    data class Error(val message: String) : CollectionPickerUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CollectionPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val collectionId: String =
        Routes.decode(savedStateHandle.get<String>("collectionId"))

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _collection = MutableStateFlow<UserCollection?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val uiState: StateFlow<CollectionPickerUiState> = combine(
        _query.debounce(300).distinctUntilChanged(),
        _collection
    ) { q, collection ->
        q.trim() to collection?.itemIds.orEmpty()
    }
        .distinctUntilChanged()
        .flatMapLatest { (q, excluded) ->
            flow {
                emit(CollectionPickerUiState.Loading)
                runCatchingCancellable {
                    val all = if (q.isBlank()) itemRepository.getTopRated()
                    else itemRepository.searchByName(q)
                    all.filterNot { it.id in excluded }
                }
                    .onSuccess { emit(CollectionPickerUiState.Success(it)) }
                    .onFailure {
                        emit(
                            CollectionPickerUiState.Error(
                                it.message ?: "Не удалось загрузить"
                            )
                        )
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionPickerUiState.Loading
        )

    init {
        if (collectionId.isNotBlank()) {
            viewModelScope.launch {
                collectionRepository.observeCollection(collectionId)
                    .catch { e ->
                        _error.value = e.message ?: "Не удалось загрузить коллекцию"
                    }
                    .collect { _collection.value = it }
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        // Ошибка добавления относится к прошлому действию — сбрасываем.
        _error.value = null
    }

    fun addItem(itemId: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            collectionRepository.addItem(collectionId, itemId)
                .onSuccess {
                    _error.value = null
                    onAdded()
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Не удалось добавить"
                }
        }
    }
}