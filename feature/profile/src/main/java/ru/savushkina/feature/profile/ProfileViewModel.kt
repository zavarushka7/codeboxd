package ru.savushkina.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.data.AuthRepository
import ru.savushkina.core.data.UserRepository
import ru.savushkina.core.model.User
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Не авторизован")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val user = userRepository.getUser(uid)
            _uiState.value = if (user != null) {
                ProfileUiState.Success(user)
            } else {
                ProfileUiState.Error("Профиль не найден")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}