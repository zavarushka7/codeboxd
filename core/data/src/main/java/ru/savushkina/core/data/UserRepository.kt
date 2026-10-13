package ru.savushkina.core.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getUser(uid: String): User? = runCatching {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) return@runCatching null

        val roleString: String? = doc.getString("role")
        val role: Role? = roleString?.let { s ->
            runCatching { Role.valueOf(s) }.getOrNull()
        }

        User(
            uid = uid,
            email = doc.getString("email") ?: "",
            name = doc.getString("name") ?: "",
            role = role,
            onboardingCompleted = doc.getBoolean("onboardingCompleted") ?: false
        )
    }.getOrNull()

    suspend fun saveOnboarding(
        uid: String,
        email: String,
        name: String,
        role: Role
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "email" to email,
            "name" to name,
            "role" to role.name,   // сохраняем ключ enum, например "DEVELOPMENT"
            "onboardingCompleted" to true
        )
        firestore.collection("users").document(uid).set(data).await()
        Unit
    }
}