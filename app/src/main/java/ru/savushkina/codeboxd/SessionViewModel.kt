package ru.savushkina.codeboxd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState

    private val _retrying = MutableStateFlow(false)
    val retrying: StateFlow<Boolean> = _retrying.asStateFlow()

    val currentUserId: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                is SessionState.Authenticated -> state.user.uid
                is SessionState.Onboarding -> state.userId
                is SessionState.ProfileLoadError -> state.userId
                else -> null
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val navTarget: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                SessionState.Unknown -> null
                SessionState.LoadingProfile -> null
                is SessionState.ProfileLoadError -> null
                SessionState.Unauthenticated -> Routes.AUTH
                is SessionState.Onboarding -> Routes.ONBOARDING
                is SessionState.Authenticated -> Routes.FEED
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * true — на сплэше нужно показать «повторить».
     * Скрываем кнопку на время попытки (_retrying = true).
     */
    val canRetry: StateFlow<Boolean> = combine(
        sessionRepository.sessionState,
        _retrying
    ) { state, retrying ->
        state is SessionState.ProfileLoadError && !retrying
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun retry() {
        val state = sessionState.value
        val uid = when (state) {
            is SessionState.ProfileLoadError -> state.userId
            is SessionState.Authenticated -> state.user.uid
            is SessionState.Onboarding -> state.userId
            else -> return
        }
        if (_retrying.value) return
        viewModelScope.launch {
            _retrying.value = true
            try {
                userRepository.refresh(uid)
            } finally {
                _retrying.value = false
            }
        }
    }
}