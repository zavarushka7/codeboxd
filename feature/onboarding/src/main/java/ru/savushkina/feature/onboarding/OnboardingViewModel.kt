package ru.savushkina.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val username: String = "",
    val role: Role? = null,
    val isLoading: Boolean = false,
    val isCheckingUsername: Boolean = false,
    val usernameAvailable: Boolean? = null,
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

    /** Текущая корутина проверки username — чтобы отменять при новом вводе. */
    private var usernameCheckJob: Job? = null

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onUsernameChange(value: String) {
        val clean = value.lowercase().removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }
        _state.update {
            it.copy(
                username = clean,
                error = null,
                usernameAvailable = null,
                isCheckingUsername = false
            )
        }
        scheduleUsernameCheck(clean)
    }

    private fun scheduleUsernameCheck(username: String) {
        usernameCheckJob?.cancel()
        if (username.isBlank()) return

        val validation = validateUsername(username)
        if (validation != null) {
            _state.update { it.copy(usernameAvailable = false) }
            return
        }

        usernameCheckJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isCheckingUsername = true, usernameAvailable = null) }
            userRepository.isUsernameAvailable(username)
                .onSuccess { available ->
                    _state.update {
                        it.copy(
                            isCheckingUsername = false,
                            usernameAvailable = available,
                            error = if (available) null else "Этот username уже занят"
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isCheckingUsername = false,
                            usernameAvailable = null,
                            error = it.error ?: "Не удалось проверить username"
                        )
                    }
                }
        }
    }

    fun onRoleSelect(role: Role) {
        _state.update { it.copy(role = role, error = null) }
    }

    fun complete() {
        val name = _state.value.name.trim()
        val username = _state.value.username.trim()
        val role = _state.value.role

        validate(name, username, role)?.let {
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
                    username = username,
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

    fun signOut() {
        sessionRepository.signOut()
    }

    private fun validate(name: String, username: String, role: Role?): String? {
        if (name.isBlank()) return "Введите имя"
        if (name.length > UserConstraints.NAME_MAX) {
            return "Имя слишком длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        validateUsername(username)?.let { return it }
        if (role == null) return "Выберите роль"
        return null
    }

    private fun validateUsername(username: String): String? {
        val clean = username.trim().lowercase().removePrefix("@")
        return when {
            clean.isBlank() -> "Придумайте username"
            clean.length < UserConstraints.USERNAME_MIN ->
                "Username слишком короткий (минимум ${UserConstraints.USERNAME_MIN})"
            clean.length > UserConstraints.USERNAME_MAX ->
                "Username слишком длинный (максимум ${UserConstraints.USERNAME_MAX})"
            !UserConstraints.USERNAME_REGEX.matches(clean) ->
                "Только lowercase латиница, цифры и _"
            else -> null
        }
    }
}