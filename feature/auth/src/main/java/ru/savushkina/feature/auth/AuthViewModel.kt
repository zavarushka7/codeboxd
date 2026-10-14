package ru.savushkina.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthErrorMapper
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.UserConstraints
import javax.inject.Inject

data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(
            isPasswordVisible = !_state.value.isPasswordVisible
        )
    }

    fun signIn(onSuccess: () -> Unit) {
        val email = _state.value.email.trim()
        val password = _state.value.password

        validate(email, password)?.let {
            _state.value = _state.value.copy(error = it)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            authRepository.signIn(email, password)
                .onSuccess {
                    val uid = authRepository.currentUserId
                    if (uid == null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "не_удалось_войти"
                        )
                        return@onSuccess
                    }

                    userRepository.refresh(uid)
                        .onSuccess {
                            // Не сбрасываем isLoading — экран уйдёт сам,
                            // как только SessionState переключится.
                            // isDone защищает от мелькания формы между
                            // завершением запроса и навигацией.
                            _state.value = _state.value.copy(isDone = true)
                            onSuccess()
                        }
                        .onFailure {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "не_удалось_загрузить_профиль"
                            )
                        }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = AuthErrorMapper.map(it).message
                    )
                }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val email = _state.value.email.trim()
        val password = _state.value.password

        validate(email, password)?.let {
            _state.value = _state.value.copy(error = it)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            authRepository.signUp(email, password)
                .onSuccess {
                    val uid = authRepository.currentUserId
                    if (uid == null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "не_удалось_создать_аккаунт"
                        )
                        return@onSuccess
                    }

                    userRepository.ensureUserExists(uid, email)
                        .onSuccess {
                            // См. комментарий в signIn
                            _state.value = _state.value.copy(isDone = true)
                            onSuccess()
                        }
                        .onFailure {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "не_удалось_создать_профиль"
                            )
                        }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = AuthErrorMapper.map(it).message
                    )
                }
        }
    }

    private fun validate(email: String, password: String): String? {
        if (email.isBlank()) return "введите_email"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "некорректный_email"
        }
        if (password.length < UserConstraints.PASSWORD_MIN) {
            return "пароль_слишком_короткий (минимум ${UserConstraints.PASSWORD_MIN} символов)"
        }
        if (password.length > UserConstraints.PASSWORD_MAX) {
            return "пароль_слишком_длинный (максимум ${UserConstraints.PASSWORD_MAX} символов)"
        }
        return null
    }
}