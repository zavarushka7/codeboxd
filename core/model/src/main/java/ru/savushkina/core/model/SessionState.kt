package ru.savushkina.core.model

sealed interface SessionState {
    data object Unknown : SessionState
    data object Unauthenticated : SessionState
    /** Аутентифицирован, но профиль ещё грузится. */
    data object LoadingProfile : SessionState
    /** Аутентифицирован, но не удалось загрузить профиль (offline, ошибка). */
    data class ProfileLoadError(val userId: String) : SessionState
    /** Пользователь вошёл, но не завершил онбординг. */
    data class Onboarding(val userId: String) : SessionState
    /** Полностью вошёл и онбординг пройден. */
    data class Authenticated(val user: User) : SessionState
}