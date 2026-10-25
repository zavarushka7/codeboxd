package ru.savushkina.feature.suggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.SuggestionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.ItemSuggestion
import javax.inject.Inject

data class SuggestionFormState(
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val isSending: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SuggestionViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestionFormState())
    val state = _state.asStateFlow()

    fun onNameChange(v: String) {
        _state.update { it.copy(name = v, error = null) }
    }

    fun onTypeChange(v: String) {
        _state.update { it.copy(type = v, error = null) }
    }

    fun onDescriptionChange(v: String) {
        _state.update { it.copy(description = v, error = null) }
    }

    /** Сбрасываем isDone после обработки UI-события (закрытие диалога). */
    fun onDoneHandled() {
        if (_state.value.isDone) {
            _state.update { it.copy(isDone = false) }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.isSending) return

        validate(s)?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }

            runCatchingCancellable {
                val uid = authRepository.currentUid()
                    ?: throw IllegalStateException("Не авторизован")

                val author = userRepository.currentUser.value

                suggestionRepository.submitSuggestion(
                    ItemSuggestion(
                        authorUid = uid,
                        authorName = author?.displayName ?: "",
                        name = s.name.trim(),
                        type = s.type.trim(),
                        description = s.description.trim()
                    )
                ).getOrThrow()
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSending = false,
                        isDone = true,
                        name = "",
                        type = "",
                        description = ""
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isSending = false,
                        error = e.message ?: "Не удалось отправить заявку"
                    )
                }
            }
        }
    }

    private fun validate(s: SuggestionFormState): String? {
        val name = s.name.trim()
        val type = s.type.trim()
        val desc = s.description.trim()

        if (name.isBlank()) return "Введите имя айтема"
        if (name.length > ItemSuggestion.NAME_MAX) {
            return "Имя слишком длинное (макс. ${ItemSuggestion.NAME_MAX})"
        }
        if (type.isBlank()) return "Введите тип"
        if (type.length > ItemSuggestion.TYPE_MAX) {
            return "Тип слишком длинный (макс. ${ItemSuggestion.TYPE_MAX})"
        }
        if (desc.length > ItemSuggestion.DESCRIPTION_MAX) {
            return "Описание слишком длинное (макс. ${ItemSuggestion.DESCRIPTION_MAX})"
        }
        return null
    }
}