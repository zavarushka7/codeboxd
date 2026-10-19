// core/domain/src/main/java/ru/savushkina/core/domain/AuthRepository.kt
package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.AuthUser


interface AuthRepository {
    val authState: Flow<AuthUser?>

    /** Синхронно возвращает uid текущего пользователя (null если не залогинен). */
    fun currentUid(): String?

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    fun signOut()
}