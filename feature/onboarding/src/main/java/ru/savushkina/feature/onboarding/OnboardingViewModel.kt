package ru.savushkina.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.data.AuthRepository
import ru.savushkina.core.data.UserRepository
import ru.savushkina.core.model.Role
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun onRoleSelect(role: Role) {
        _state.value = _state.value.copy(role = role, error = null)
    }

    fun complete(onDone: () -> Unit) {
        val name = _state.value.name.trim()
        val role = _state.value.role

        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Введите имя")
            return
        }
        if (role == null) {
            _state.value = _state.value.copy(error = "Выберите роль")
            return
        }

        val uid = authRepository.currentUserId
        val email = authRepository.currentUserEmail ?: ""
        if (uid == null) {
            _state.value = _state.value.copy(error = "Не авторизован")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            userRepository.saveOnboarding(uid, email, name, role)
                .onSuccess { onDone() }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "Ошибка сохранения"
                    )
                }
        }
    }
}