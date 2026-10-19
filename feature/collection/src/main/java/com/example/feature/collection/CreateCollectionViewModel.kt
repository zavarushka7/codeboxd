package com.example.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
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
        _state.update { it.copy(name = value, error = null) }
    }

    fun onKindChange(kind: CollectionKind) {
        _state.update {
            it.copy(
                kind = kind,
                allowedType = if (kind == CollectionKind.ARRAY) it.allowedType else null,
                error = null
            )
        }
    }

    fun onAllowedTypeChange(type: String) {
        _state.update { it.copy(allowedType = type, error = null) }
    }

    fun createList(onSuccess: (String) -> Unit) {
        val s = _state.value
        val validationError = validateName(s.name)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        if (s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatchingCancellable {
                val uid = authRepository.authState.firstOrNull()?.uid
                    ?: throw IllegalStateException("Не авторизован")
                val collection = UserCollection(
                    userId = uid,
                    name = s.name.trim(),
                    itemIds = emptyList(),
                    isFixedSize = false,
                    maxSize = 0,
                    allowedItemType = null,
                    createdAtMillis = System.currentTimeMillis()
                )
                collectionRepository.createCollection(collection).getOrThrow()
            }.onSuccess { id ->
                _state.update { it.copy(isLoading = false) }
                onSuccess(id)
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось создать"
                    )
                }
            }
        }
    }

    fun validateForArray(): Boolean {
        val s = _state.value
        val nameError = validateName(s.name)
        if (nameError != null) {
            _state.update { it.copy(error = nameError) }
            return false
        }
        if (s.allowedType.isNullOrBlank()) {
            _state.update { it.copy(error = "Выберите тип айтемов") }
            return false
        }
        return true
    }

    private fun validateName(raw: String): String? {
        val name = raw.trim()
        return when {
            name.isBlank() -> "Введите имя"
            name.length > UserCollection.NAME_MAX ->
                "Имя слишком длинное (макс. ${UserCollection.NAME_MAX})"
            else -> null
        }
    }
}