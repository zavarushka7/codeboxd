package com.example.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.model.UserCollection
import ru.savushkina.core.navigation.Routes
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

    private val collectionId: String =
        Routes.decode(savedStateHandle.get<String>("collectionId"))

    private val _uiState = MutableStateFlow<CollectionDetailUiState>(
        CollectionDetailUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    private val _retry = MutableStateFlow(0)

    /**
     * UID текущего пользователя как StateFlow. Никакого var вне State —
     * UI гарантированно рекомпозируется при смене авторизации.
     */
    private val currentUserId: StateFlow<String?> = authRepository.authState
        .map { it?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = authRepository.currentUid()
        )

    /**
     * true, если текущая коллекция принадлежит текущему пользователю.
     * Пересчитывается автоматически при изменении _uiState или currentUserId.
     */
    val isOwnCollection: StateFlow<Boolean> = combine(
        _uiState,
        currentUserId
    ) { state, uid ->
        state is CollectionDetailUiState.Success &&
                uid != null &&
                state.collection.userId == uid
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    init {
        if (collectionId.isBlank()) {
            _uiState.value = CollectionDetailUiState.Error("Некорректный ID коллекции")
        } else {
            observe()
        }
    }

    private fun observe() {
        viewModelScope.launch {
            _retry.flatMapLatest {
                _uiState.value = CollectionDetailUiState.Loading
                collectionRepository.observeCollection(collectionId)
                    .catch { e ->
                        _uiState.value = CollectionDetailUiState.Error(
                            e.message ?: "Не удалось загрузить коллекцию"
                        )
                        emit(null)
                    }
            }.collect { collection ->
                if (collection == null) {
                    // Ошибку уже показали в catch; повторно не затираем
                    return@collect
                }
                runCatchingCancellable {
                    itemRepository.getItemsByIds(collection.itemIds)
                }.onSuccess { items ->
                    _uiState.value = CollectionDetailUiState.Success(collection, items)
                }.onFailure {
                    _uiState.value = CollectionDetailUiState.Error(
                        it.message ?: "Не удалось загрузить айтемы"
                    )
                }
            }
        }
    }

    fun retry() {
        _retry.value++
    }

    fun removeItem(itemId: String) {
        if (!isOwnCollection.value) return
        viewModelScope.launch {
            collectionRepository.removeItem(collectionId, itemId)
                .onFailure {
                    _uiState.value = CollectionDetailUiState.Error(
                        it.message ?: "Не удалось удалить айтем"
                    )
                }
        }
    }

    fun deleteCollection(onDeleted: () -> Unit) {
        if (!isOwnCollection.value) return
        viewModelScope.launch {
            collectionRepository.deleteCollection(collectionId)
                .onSuccess { onDeleted() }
                .onFailure {
                    _uiState.value = CollectionDetailUiState.Error(
                        it.message ?: "Не удалось удалить коллекцию"
                    )
                }
        }
    }
}