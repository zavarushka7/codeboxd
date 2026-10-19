package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuthException
import ru.savushkina.core.domain.ErrorMapper
import ru.savushkina.core.model.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthErrorMapper @Inject constructor() : ErrorMapper {

    override fun map(e: Throwable): AppError = mapInternal(e)

    companion object {
        fun mapInternal(e: Throwable): AppError {
            if (e is FirebaseAuthException) {
                val code = e.errorCode ?: ""
                val message = when (code) {
                    "ERROR_INVALID_CREDENTIALS",
                    "INVALID_LOGIN_CREDENTIALS" -> "Неверный email или пароль"

                    "ERROR_EMAIL_ALREADY_IN_USE",
                    "EMAIL_EXISTS" -> "Этот email уже зарегистрирован"

                    "ERROR_WEAK_PASSWORD",
                    "WEAK_PASSWORD" -> "Пароль слишком короткий (минимум 6 символов)"

                    "ERROR_USER_NOT_FOUND",
                    "USER_NOT_FOUND" -> "Пользователь с таким email не найден"

                    "ERROR_INVALID_EMAIL",
                    "INVALID_EMAIL" -> "Некорректный email"

                    "ERROR_TOO_MANY_REQUESTS",
                    "TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте позже"

                    "ERROR_NETWORK_REQUEST_FAILED",
                    "NETWORK_ERROR" -> "Нет подключения к интернету"

                    "ERROR_OPERATION_NOT_ALLOWED",
                    "OPERATION_NOT_ALLOWED" -> "Вход по email временно отключён"

                    else -> fallbackByMessage(e.message.orEmpty())
                }
                return AppError.Auth(message = message, cause = e)
            }

            val msg = e.message.orEmpty()
            return when {
                msg.contains("network", ignoreCase = true) ->
                    AppError.Network(cause = e)
                else -> AppError.Unknown(cause = e)
            }
        }

        private fun fallbackByMessage(msg: String): String = when {
            msg.contains("credential is incorrect", ignoreCase = true) ->
                "Неверный email или пароль"
            msg.contains("network", ignoreCase = true) ->
                "Нет подключения к интернету"
            else -> "Что-то пошло не так. Попробуйте ещё раз"
        }
    }
}