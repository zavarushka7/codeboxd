package ru.savushkina.core.model

object UserConstraints {
    const val NAME_MIN = 3
    const val NAME_MAX = 24
    const val DESCRIPTION_MAX = 500
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 64
    const val AVATAR_MAX_BYTES = 900_000
}