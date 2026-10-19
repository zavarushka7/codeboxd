package ru.savushkina.core.model

/**
 * Доменная ошибка с готовым сообщением для UI.
 * Заменяет core/common/AppError.
 */
sealed interface AppError {
    val message: String
    val cause: Throwable?

    data class Network(
        override val message: String = "Нет подключения к интернету",
        override val cause: Throwable? = null
    ) : AppError

    data class Auth(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError

    data class Validation(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError

    data class NotFound(
        override val message: String = "Не найдено",
        override val cause: Throwable? = null
    ) : AppError

    data class LimitReached(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError

    data class Unknown(
        override val message: String = "Что-то пошло не так. Попробуйте ещё раз",
        override val cause: Throwable? = null
    ) : AppError
}