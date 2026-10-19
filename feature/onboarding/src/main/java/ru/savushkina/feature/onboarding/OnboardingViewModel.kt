package ru.savushkina.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.UserConstraints
import javax.inject.Inject

data class OnboardingState(
    val name: String = "",
    val role: Role? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onRoleSelect(role: Role) {
        _state.update { it.copy(role = role, error = null) }
    }

    fun complete() {
        val name = _state.value.name.trim()
        val role = _state.value.role

        validate(name, role)?.let {
            _state.update { s -> s.copy(error = it) }
            return
        }
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatchingCancellable {
                val authUser = authRepository.authState.filterNotNull().first()
                val email = authUser.email
                    ?: throw IllegalStateException("Email не найден в профиле")

                userRepository.saveOnboarding(
                    uid = authUser.uid,
                    email = email,
                    name = name,
                    role = role!!
                ).getOrThrow()
            }.onSuccess {
                _state.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось сохранить"
                    )
                }
            }
        }
    }

    /** Выход из аккаунта: возвращает пользователя на экран auth. */
    fun signOut() {
        sessionRepository.signOut()
    }

    private fun validate(name: String, role: Role?): String? {
        if (name.isBlank()) return "Введите имя"
        if (name.length > UserConstraints.NAME_MAX) {
            return "Имя слишком длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        if (role == null) return "Выберите роль"
        return null
    }
}