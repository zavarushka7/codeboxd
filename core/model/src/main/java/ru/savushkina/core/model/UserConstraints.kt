package ru.savushkina.core.model

object UserConstraints {
    const val NAME_MIN = 3
    const val NAME_MAX = 24
    const val DESCRIPTION_MAX = 500
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 64

    /** Максимальная длина base64-строки аватара (ASCII, 1 символ = 1 байт). */
    const val AVATAR_MAX_BASE64_LENGTH = 700_000

    @Deprecated("Use AVATAR_MAX_BASE64_LENGTH", ReplaceWith("AVATAR_MAX_BASE64_LENGTH"))
    const val AVATAR_MAX_BYTES = AVATAR_MAX_BASE64_LENGTH
}