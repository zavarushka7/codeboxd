package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.SessionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepositoryImpl
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var lastHandledUid: String? = null

    init {
        val initialUid = auth.currentUser?.uid
        if (initialUid != null) {
            lastHandledUid = initialUid
            scope.launch { userRepository.refresh(initialUid) }
        } else {
            _sessionState.value = SessionState.Unauthenticated
        }

        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid

            if (uid == null) {
                lastHandledUid = null
                userRepository.clear()
                _sessionState.value = SessionState.Unauthenticated
            } else if (uid != lastHandledUid) {
                lastHandledUid = uid
                scope.launch { userRepository.refresh(uid) }
            }
        }

        scope.launch {
            userRepository.currentUser.collect { user ->
                val newState = when {
                    user == null -> {
                        if (auth.currentUser == null) SessionState.Unauthenticated
                        else _sessionState.value
                    }
                    !user.onboardingCompleted -> SessionState.Onboarding(user.uid)
                    else -> SessionState.Authenticated(user)
                }

                val currentClass = _sessionState.value::class
                val newClass = newState::class

                if (currentClass != newClass) {
                    _sessionState.value = newState
                } else if (newState is SessionState.Authenticated &&
                    _sessionState.value is SessionState.Authenticated
                ) {
                    val oldUser = (_sessionState.value as SessionState.Authenticated).user
                    if (oldUser != user) {
                        _sessionState.value = newState
                    }
                }
            }
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}