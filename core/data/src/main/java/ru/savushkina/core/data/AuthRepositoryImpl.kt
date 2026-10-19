// core/data/src/main/java/ru/savushkina/core/data/AuthRepositoryImpl.kt
package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.model.AuthUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUid(): String? = auth.currentUser?.uid

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

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)
}