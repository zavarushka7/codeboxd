package ru.savushkina.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.data.AuthRepository
import ru.savushkina.core.data.UserRepository
import javax.inject.Inject

data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null
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
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }
    fun signIn(onDone: (Boolean) -> Unit) {
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
                    // Сбрасываем isLoading ПЕРЕД переходом
                    _state.value = _state.value.copy(isLoading = false, error = null)
                    val uid = authRepository.currentUserId
                    val user = uid?.let { userRepository.getUser(it) }
                    onDone(user?.onboardingCompleted == true)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = mapFirebaseError(it)
                    )
                }
        }
    }

    fun signUp(onDone: () -> Unit) {
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
                    // Сбрасываем isLoading ПЕРЕД переходом
                    _state.value = _state.value.copy(isLoading = false, error = null)
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = mapFirebaseError(it)
                    )
                }
        }
    }
    private fun validate(email: String, password: String): String? {
        if (email.isBlank()) return "Введите email"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Некорректные данные"
        }
        if (password.length < 6) return "Пароль слишком короткий (минимум 6 символов"
        if (password.length > 64) return "Пароль слишком длинный (максимум 64 символа)"
        return null
    }



    private fun mapFirebaseError(e: Throwable): String {
        // Сначала пробуем вытащить код ошибки Firebase Auth
        val errorCode = (e as? FirebaseAuthException)?.errorCode

        return when (errorCode) {
            // Неверный email или пароль (современный Firebase не различает)
            "ERROR_INVALID_CREDENTIALS",
            "INVALID_LOGIN_CREDENTIALS" -> "Неверный email или пароль"

            // Email уже занят при регистрации
            "ERROR_EMAIL_ALREADY_IN_USE",
            "EMAIL_EXISTS" -> "Этот email уже зарегистрирован"

            // Пароль слишком слабый (обычно < 6 символов)
            "ERROR_WEAK_PASSWORD",
            "WEAK_PASSWORD" -> "Пароль слишком короткий (минимум 6 символов)"

            // Пользователь не найден (при регистрации, если что-то странное)
            "ERROR_USER_NOT_FOUND",
            "USER_NOT_FOUND" -> "Пользователь с таким email не найден"

            // Некорректный email
            "ERROR_INVALID_EMAIL",
            "INVALID_EMAIL" -> "Некорректный email"

            // Слишком много попыток
            "ERROR_TOO_MANY_REQUESTS",
            "TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте позже"

            // Сеть
            "ERROR_NETWORK_REQUEST_FAILED",
            "NETWORK_ERROR" -> "Нет подключения к интернету"

            // Вход по email отключён в консоли
            "ERROR_OPERATION_NOT_ALLOWED",
            "OPERATION_NOT_ALLOWED" -> "Вход по email временно отключён"

            // Фолбэк по тексту сообщения (на всякий случай)
            else -> {
                val msg = e.message ?: ""
                when {
                    msg.contains("credential is incorrect", ignoreCase = true) ->
                        "Неверный email или пароль"
                    msg.contains("network", ignoreCase = true) ->
                        "Нет подключения к интернету"
                    else -> "Что-то пошло не так. Попробуйте ещё раз"
                }
            }
        }
    }
}