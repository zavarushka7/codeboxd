package ru.savushkina.core.common

/**
 * Доменная ошибка с готовым сообщением для UI.
 * Репозитории возвращают этот тип вместо Throwable.
 */
data class AppError(
    val message: String,
    val cause: Throwable? = null
) {
    companion object {
        fun unknown(): AppError = AppError("Что-то пошло не так. Попробуйте ещё раз")
        fun network(): AppError = AppError("Нет подключения к интернету")
    }
}