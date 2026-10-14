package ru.savushkina.core.domain

import kotlinx.coroutines.flow.StateFlow
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User

interface UserRepository {
    val currentUser: StateFlow<User?>

    suspend fun refresh(uid: String): Result<User>
    suspend fun getUser(uid: String): User?
    suspend fun ensureUserExists(uid: String, email: String): Result<User>

    suspend fun saveOnboarding(
        uid: String,
        email: String,
        name: String,
        role: Role
    ): Result<Unit>

    suspend fun saveProfile(
        uid: String,
        name: String,
        description: String,
        avatarBase64: String,
        role: Role?
    ): Result<Unit>
}