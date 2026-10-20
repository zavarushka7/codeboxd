package ru.savushkina.core.model

object UserConstraints {
    const val NAME_MIN = 3
    const val NAME_MAX = 24
    const val DESCRIPTION_MAX = 500
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 64

    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    /** Только lowercase latin, цифры и подчёркивание. */
    val USERNAME_REGEX = Regex("^[a-z0-9_]{$USERNAME_MIN,$USERNAME_MAX}$")

    const val AVATAR_MAX_BASE64_LENGTH = 700_000
}