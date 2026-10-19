package com.example.feature.favourite_picker

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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.FavouriteRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject

sealed interface FavouritePickerUiState {
    data object Idle : FavouritePickerUiState
    data object Loading : FavouritePickerUiState
    data class Success(val items: List<Item>) : FavouritePickerUiState
    data class Error(val message: String) : FavouritePickerUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class FavouritePickerViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val favouriteRepository: FavouriteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    /** Ошибка загрузки списка избранного (не блокирует работу). */
    private val _favouritesLoadError = MutableStateFlow<String?>(null)
    val favouritesLoadError: StateFlow<String?> = _favouritesLoadError.asStateFlow()

    val uiState: StateFlow<FavouritePickerUiState> = _query
        .debounce(300)
        .distinctUntilChanged()
        .combine(_selectedIds) { q, selected -> q.trim() to selected }
        .distinctUntilChanged()
        .flatMapLatest { (q, selected) ->
            flow {
                emit(FavouritePickerUiState.Loading)
                runCatchingCancellable {
                    if (q.isBlank()) itemRepository.getTopRated()
                    else itemRepository.searchByName(q)
                }
                    .onSuccess { items ->
                        emit(
                            FavouritePickerUiState.Success(
                                items.filterNot { it.id in selected }
                            )
                        )
                    }
                    .onFailure {
                        emit(
                            FavouritePickerUiState.Error(
                                it.message ?: "Не удалось загрузить"
                            )
                        )
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FavouritePickerUiState.Loading
        )

    init {
        viewModelScope.launch {
            val uid = authRepository.authState.firstOrNull()?.uid
            if (uid == null) {
                _favouritesLoadError.value = "Не авторизован"
                return@launch
            }
            favouriteRepository.observeFavourites(uid)
                .catch { e ->
                    // Не роняем экран — показываем ошибку отдельно.
                    _favouritesLoadError.value =
                        e.message ?: "Не удалось загрузить избранное"
                }
                .collect { ids ->
                    _selectedIds.value = ids.toSet()
                    _favouritesLoadError.value = null
                }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }
}