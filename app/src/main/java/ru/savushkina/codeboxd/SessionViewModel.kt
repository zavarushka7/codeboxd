package ru.savushkina.codeboxd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.SessionState
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState

    val currentUserId: String? = authRepository.currentUserId

    val navTarget: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                SessionState.Unknown -> null
                SessionState.Unauthenticated -> Routes.AUTH
                is SessionState.Onboarding -> Routes.ONBOARDING
                is SessionState.Authenticated -> Routes.FEED
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
}