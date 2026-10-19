package ru.savushkina.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.savushkina.core.data.di.ApplicationScope
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.SessionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    @ApplicationScope appScope: CoroutineScope
) : SessionRepository {

    override val sessionState: StateFlow<SessionState> =
        combine(
            authRepository.authState,
            userRepository.currentUser,
            userRepository.profileLoading,
            userRepository.profileError
        ) { auth, user, loading, error ->
            when {
                auth == null -> SessionState.Unauthenticated
                user != null && user.onboardingCompleted -> SessionState.Authenticated(user)
                user != null -> SessionState.Onboarding(user.uid)
                loading -> SessionState.LoadingProfile
                error != null -> SessionState.ProfileLoadError(auth.uid)
                else -> SessionState.LoadingProfile
            }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Unknown
        )

    override fun signOut() = authRepository.signOut()
}