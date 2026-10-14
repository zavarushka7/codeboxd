package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatchingCancellable {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatchingCancellable {
            auth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }

    override fun signOut() {
        auth.signOut()
    }
}