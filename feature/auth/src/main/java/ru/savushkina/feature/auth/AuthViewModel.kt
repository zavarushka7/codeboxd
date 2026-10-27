package ru.savushkina.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ErrorMapper
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.UserConstraints
import javax.inject.Inject

data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isDone: Boolean = false,
    val fieldError: String? = null,
    val submitError: String? = null
) {
    val error: String?
        get() = fieldError ?: submitError
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, fieldError = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, fieldError = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }

    /**
     * Вызывается UI после обработки [AuthState.isDone] (например, из
     * DisposableEffect.onDispose при уходе с экрана). Сбрасываем оба флага —
     * isDone и isLoading — на случай, если по какой-то причине isLoading
     * остался true. UI после этого снова покажет форму, что корректно:
     * если пользователь вернулся на AuthScreen, он ожидает увидеть форму.
     */
    fun onDoneHandled() {
        val current = _state.value
        if (current.isDone || current.isLoading) {
            _state.value = current.copy(isDone = false, isLoading = false)
        }
    }

    fun signIn() {
        if (_state.value.isLoading) return

        val email = _state.value.email.trim()
        val password = _state.value.password
        validate(email, password)?.let { message ->
            _state.value = _state.value.copy(fieldError = message, submitError = null)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                isDone = false,
                fieldError = null,
                submitError = null
            )

            val signInResult = runCatchingCancellable {
                authRepository.signIn(email, password)
            }.getOrElse { e ->
                handleUnexpectedFailure(e)
                return@launch
            }

            signInResult.fold(
                onSuccess = {
                    val profileResult = runCatchingCancellable {
                        val uid = authRepository.currentUid()
                            ?: throw IllegalStateException("Нет uid после успешного входа")
                        userRepository.ensureUserExists(uid, email)
                    }.getOrElse { e ->
                        handleUnexpectedFailure(e)
                        return@launch
                    }

                    profileResult.fold(
                        onSuccess = {
                            _state.value = _state.value.copy(isLoading = false, isDone = true)
                        },
                        onFailure = { e ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isDone = false,
                                submitError = errorMapper.map(e).message
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isDone = false,
                        submitError = errorMapper.map(e).message
                    )
                }
            )
        }
    }

    fun signUp() {
        if (_state.value.isLoading) return

        val email = _state.value.email.trim()
        val password = _state.value.password
        validate(email, password)?.let { message ->
            _state.value = _state.value.copy(fieldError = message, submitError = null)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                isDone = false,
                fieldError = null,
                submitError = null
            )

            val signUpResult = runCatchingCancellable {
                authRepository.signUp(email, password)
            }.getOrElse { e ->
                handleUnexpectedFailure(e)
                return@launch
            }

            signUpResult.fold(
                onSuccess = {
                    val profileResult = runCatchingCancellable {
                        val uid = authRepository.currentUid()
                            ?: throw IllegalStateException("Нет uid после успешной регистрации")
                        userRepository.ensureUserExists(uid, email)
                    }.getOrElse { e ->
                        handleUnexpectedFailure(e)
                        return@launch
                    }

                    profileResult.fold(
                        onSuccess = {
                            _state.value = _state.value.copy(isLoading = false, isDone = true)
                        },
                        onFailure = { e ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isDone = false,
                                submitError = errorMapper.map(e).message
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isDone = false,
                        submitError = errorMapper.map(e).message
                    )
                }
            )
        }
    }

    private fun handleUnexpectedFailure(e: Throwable) {
        _state.value = _state.value.copy(
            isLoading = false,
            isDone = false,
            submitError = errorMapper.map(e).message
        )
    }

    private fun validate(email: String, password: String): String? {
        if (email.isBlank()) return "Введите email"
        if (email.length > EMAIL_MAX_LENGTH) return "Email слишком длинный"
        if (!EMAIL_REGEX.matches(email)) return "Некорректный email"
        if (password.length < UserConstraints.PASSWORD_MIN) {
            return "Пароль слишком короткий (минимум ${UserConstraints.PASSWORD_MIN})"
        }
        if (password.length > UserConstraints.PASSWORD_MAX) {
            return "Пароль слишком длинный (максимум ${UserConstraints.PASSWORD_MAX})"
        }
        return null
    }

    private companion object {
        const val EMAIL_MAX_LENGTH = 254

        val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
    }
}