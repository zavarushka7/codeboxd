package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
){
    val currentUserId: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }
    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    fun signOut() {
        auth.signOut()
    }
}