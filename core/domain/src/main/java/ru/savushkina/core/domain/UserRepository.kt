package ru.savushkina.core.domain

import kotlinx.coroutines.flow.StateFlow
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User

interface UserRepository {
    val currentUser: StateFlow<User?>
    val profileLoading: StateFlow<Boolean>
    val profileError: StateFlow<Throwable?>

    suspend fun refresh(uid: String): Result<User>
    suspend fun getUser(uid: String): User?
    suspend fun getUsersByIds(ids: List<String>): List<User>

    suspend fun getUserByUsername(username: String): User?
    suspend fun searchUsersByUsernamePrefix(prefix: String, limit: Long = 20): List<User>
    suspend fun isUsernameAvailable(username: String): Result<Boolean>

    suspend fun ensureUserExists(uid: String, email: String): Result<User>

    suspend fun saveOnboarding(
        uid: String,
        email: String,
        name: String,
        username: String,
        role: Role
    ): Result<Unit>

    /**
     * Сохраняет профиль. Если [username] не null и отличается от текущего —
     * атомарно меняет username (освобождает старый, занимает новый).
     */
    suspend fun saveProfile(
        uid: String,
        name: String,
        username: String?,
        description: String,
        avatarBase64: String,
        role: Role?
    ): Result<Unit>
}