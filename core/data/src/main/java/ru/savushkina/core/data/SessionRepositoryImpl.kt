package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.model.SessionState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единый источник правды о сессии.
 *
 * Слушает FirebaseAuth + currentUser репозитория и пересчитывает SessionState.
 * Никто больше не должен вручную вызывать clear() или пересчитывать сессию.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepositoryImpl
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                userRepository.clear()
                _sessionState.value = SessionState.Unauthenticated
            }
            // При signIn/signUp специально ничего не делаем —
            // refresh вызовется из ViewModel
        }

        scope.launch {
            userRepository.currentUser.collect { user ->
                _sessionState.value = when {
                    user == null -> SessionState.Unauthenticated
                    !user.onboardingCompleted -> SessionState.Onboarding(user.uid)
                    else -> SessionState.Authenticated(user)
                }
            }
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}