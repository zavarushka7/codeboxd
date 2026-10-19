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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.UserCollection
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface ArrayPickerUiState {
    data object Loading : ArrayPickerUiState
    data class Success(val items: List<Item>) : ArrayPickerUiState
    data class Error(val message: String) : ArrayPickerUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ArrayPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val name: String = Routes.decode(savedStateHandle.get<String>("name"))
    private val allowedType: String = Routes.decode(savedStateHandle.get<String>("type"))

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds = _selectedIds.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val uiState: StateFlow<ArrayPickerUiState> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { raw ->
            val q = raw.trim().lowercase()
            flow {
                emit(ArrayPickerUiState.Loading)
                runCatchingCancellable {
                    val all = itemRepository.getItemsByType(allowedType, limit = 50)
                    if (q.isBlank()) all
                    else all.filter { it.name.lowercase().contains(q) }
                }
                    .onSuccess { emit(ArrayPickerUiState.Success(it)) }
                    .onFailure {
                        emit(
                            ArrayPickerUiState.Error(
                                it.message ?: "Не удалось загрузить"
                            )
                        )
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ArrayPickerUiState.Loading
        )

    val typeName: String get() = allowedType

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleItem(itemId: String) {
        val current = _selectedIds.value
        if (current.contains(itemId)) {
            _selectedIds.value = current - itemId
            _error.value = null
            return
        }
        if (current.size >= UserCollection.MAX_SIZE_LIMIT) {
            _error.value = "Максимум ${UserCollection.MAX_SIZE_LIMIT} айтемов"
            return
        }
        _selectedIds.value = current + itemId
        _error.value = null
    }

    fun submit(onSuccess: (String) -> Unit) {
        val selected = _selectedIds.value.distinct()
        if (selected.size < UserCollection.MIN_SIZE) {
            _error.value = "Выберите минимум ${UserCollection.MIN_SIZE} айтема"
            return
        }
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            runCatchingCancellable {
                val uid = authRepository.authState.firstOrNull()?.uid
                    ?: throw IllegalStateException("Не авторизован")
                val collection = UserCollection(
                    userId = uid,
                    name = name,
                    itemIds = selected,
                    isFixedSize = true,
                    maxSize = selected.size,
                    allowedItemType = allowedType,
                    createdAtMillis = System.currentTimeMillis()
                )
                collectionRepository.createCollection(collection).getOrThrow()
            }.onSuccess { id ->
                _isSaving.value = false
                onSuccess(id)
            }.onFailure {
                _isSaving.value = false
                _error.value = it.message ?: "Не удалось создать массив"
            }
        }
    }
}