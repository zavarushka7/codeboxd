package ru.savushkina.core.model

/**
 * Единое состояние сессии приложения.
 * Все экраны и навигация подписаны на него.
 */
sealed interface SessionState {
    /** Начальное состояние — ещё не знаем, вошёл пользователь или нет. */
    data object Unknown : SessionState

    /** Пользователь не авторизован. */
    data object Unauthenticated : SessionState

    /** Пользователь вошёл, но не завершил онбординг. */
    data class Onboarding(val userId: String) : SessionState

    /** Пользователь полностью вошёл и онбординг пройден. */
    data class Authenticated(val user: User) : SessionState
}