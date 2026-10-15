package ru.savushkina.feature.favourite_picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

@HiltViewModel
class FavouritePickerViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val favouriteRepository: FavouriteRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val userId: String? = authRepository.currentUserId

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<FavouritePickerUiState>(FavouritePickerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    /** id, уже добавленные в четвёрку. */
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        // Подписка на текущее избранное
        val uid = userId
        if (uid != null) {
            viewModelScope.launch {
                favouriteRepository.observeFavourites(uid).collect { ids ->
                    _selectedIds.value = ids.toSet()
                    // Пересчитываем список с учётом фильтра
                    refreshList()
                }
            }
        } else {
            refreshList()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        refreshList()
    }

    private fun refreshList() {
        viewModelScope.launch {
            val query = _query.value.trim()
            _uiState.value = FavouritePickerUiState.Loading

            val result = runCatching {
                if (query.isBlank()) {
                    // Показываем всё, что есть
                    itemRepository.getTopRated()
                } else {
                    itemRepository.searchByName(query)
                }
            }

            result
                .onSuccess { items ->
                    // Фильтруем уже выбранные
                    val filtered = items.filterNot { it.id in _selectedIds.value }
                    _uiState.value = FavouritePickerUiState.Success(filtered)
                }
                .onFailure {
                    _uiState.value = FavouritePickerUiState.Error(
                        it.message ?: "Не удалось загрузить"
                    )
                }
        }
    }
}