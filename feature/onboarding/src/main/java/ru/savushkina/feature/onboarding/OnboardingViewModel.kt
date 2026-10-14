package ru.savushkina.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
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

    fun complete(onSuccess: () -> Unit) {
        val name = _state.value.name.trim()
        val role = _state.value.role

        validate(name, role)?.let {
            _state.value = _state.value.copy(error = it)
            return
        }

        val uid = authRepository.currentUserId
        if (uid == null) {
            _state.value = _state.value.copy(error = "сессия_истекла, войдите_заново")
            return
        }
        val email = authRepository.currentUserEmail ?: ""

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            userRepository.saveOnboarding(uid, email, name, role!!)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "не_удалось_сохранить"
                    )
                }
        }
    }

    private fun validate(name: String, role: Role?): String? {
        if (name.isBlank()) return "введите_имя"
        if (name.length > UserConstraints.NAME_MAX) {
            return "имя_слишком_длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        if (role == null) return "выберите_роль"
        return null
    }
}