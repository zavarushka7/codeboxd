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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.model.UserCollection
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class CollectionsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val targetUserId: String? =
        Routes.decode(savedStateHandle.get<String>("userId")).takeIf { it.isNotBlank() }

    private val _currentUserId = MutableStateFlow<String?>(null)

    val isOwnCollections: StateFlow<Boolean> = _currentUserId
        .map { current ->
            current != null && (targetUserId.isNullOrBlank() || targetUserId == current)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _collections = MutableStateFlow<List<UserCollection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _retry = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _retry.flatMapLatest {
                _error.value = null

                // ВАЖНО: filterNotNull().first() — ждём реальный uid,
                // а не первый null из authState при инициализации Firebase.
                val currentUid = authRepository.authState
                    .filterNotNull()
                    .first()
                    .uid

                _currentUserId.value = currentUid
                val uid = targetUserId ?: currentUid
                if (uid.isBlank()) {
                    _error.value = "Не авторизован"
                    flowOf(emptyList())
                } else {
                    collectionRepository.observeCollections(uid)
                        .catch { e ->
                            _error.value = e.message ?: "Не удалось загрузить коллекции"
                            emit(emptyList())
                        }
                }
            }.collect { _collections.value = it }
        }
    }

    fun retry() {
        _retry.value++
    }
}