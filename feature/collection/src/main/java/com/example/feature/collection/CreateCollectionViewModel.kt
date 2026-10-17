package com.example.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.model.CollectionKind
import ru.savushkina.core.model.UserCollection
import javax.inject.Inject

data class CreateCollectionState(
    val name: String = "",
    val kind: CollectionKind = CollectionKind.LIST,
    val allowedType: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateCollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCollectionState())
    val state = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun onKindChange(kind: CollectionKind) {
        _state.value = _state.value.copy(
            kind = kind,
            allowedType = if (kind == CollectionKind.ARRAY) _state.value.allowedType else null,
            error = null
        )
    }

    fun onAllowedTypeChange(type: String) {
        _state.value = _state.value.copy(allowedType = type, error = null)
    }

    fun createList(onSuccess: (String) -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Введите имя")
            return
        }
        val uid = authRepository.currentUserId
        if (uid == null) {
            _state.value = s.copy(error = "Не авторизован")
            return
        }

        val collection = UserCollection(
            userId = uid,
            name = s.name.trim(),
            itemIds = emptyList(),
            isFixedSize = false,
            maxSize = 0,
            allowedItemType = null
        )

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            collectionRepository.createCollection(collection)
                .onSuccess { id ->
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess(id)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "Не удалось создать"
                    )
                }
        }
    }

    fun validateForArray(): Boolean {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Введите имя")
            return false
        }
        if (s.allowedType.isNullOrBlank()) {
            _state.value = s.copy(error = "Выберите тип айтемов")
            return false
        }
        return true
    }
}