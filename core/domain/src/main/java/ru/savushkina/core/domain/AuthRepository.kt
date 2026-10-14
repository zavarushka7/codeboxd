package ru.savushkina.core.domain

interface AuthRepository {
    val currentUserId: String?
    val currentUserEmail: String?
    val isLoggedIn: Boolean

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    fun signOut()
}