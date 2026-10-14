package ru.savushkina.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Безопасный runCatching, который не глотает CancellationException.
 * Используйте его во всех suspend-функциях, где нужен Result.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }