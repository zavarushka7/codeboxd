package ru.savushkina.core.domain

import kotlinx.coroutines.flow.StateFlow
import ru.savushkina.core.model.SessionState

interface SessionRepository {
    val sessionState: StateFlow<SessionState>
    fun signOut()
}