package ru.savushkina.core.domain

import ru.savushkina.core.model.AppError

interface ErrorMapper {
    fun map(e: Throwable): AppError
}