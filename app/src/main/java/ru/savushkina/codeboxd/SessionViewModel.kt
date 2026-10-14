package ru.savushkina.codeboxd

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.SessionState
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState
}