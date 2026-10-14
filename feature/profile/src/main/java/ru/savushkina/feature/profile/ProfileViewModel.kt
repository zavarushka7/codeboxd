package ru.savushkina.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.model.User
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                _uiState.value = when (session) {
                    is SessionState.Authenticated -> ProfileUiState.Success(session.user)
                    is SessionState.Onboarding -> ProfileUiState.Loading
                    SessionState.Unauthenticated -> ProfileUiState.Error("Не авторизован")
                    SessionState.Unknown -> ProfileUiState.Loading
                }
            }
        }
    }


}