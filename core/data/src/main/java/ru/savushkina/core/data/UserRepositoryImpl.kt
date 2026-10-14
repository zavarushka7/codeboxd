package ru.savushkina.core.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun refresh(uid: String): Result<User> = runCatchingCancellable {
        val user = getUser(uid)
            ?: throw IllegalStateException("Профиль не найден")
        _currentUser.value = user
        user
    }

    override suspend fun getUser(uid: String): User? = runCatchingCancellable {
        val doc = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .await()
        if (!doc.exists()) null else doc.toUser(uid)
    }.getOrNull()

    override suspend fun saveOnboarding(
        uid: String,
        email: String,
        name: String,
        role: Role
    ): Result<Unit> = runCatchingCancellable {
        val data = mapOf(
            FIELD_EMAIL to email,
            FIELD_NAME to name,
            FIELD_ROLE to role.name,
            FIELD_ONBOARDING_COMPLETED to true
        )
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(data, SetOptions.merge())
            .await()

        _currentUser.value = _currentUser.value?.copy(
            email = email,
            name = name,
            role = role,
            onboardingCompleted = true
        ) ?: User(
            uid = uid,
            email = email,
            name = name,
            role = role,
            onboardingCompleted = true
        )
    }

    override suspend fun saveProfile(
        uid: String,
        name: String,
        description: String,
        avatarBase64: String,
        role: Role?
    ): Result<Unit> = runCatchingCancellable {
        val data = mutableMapOf<String, Any>(
            FIELD_NAME to name,
            FIELD_DESCRIPTION to description,
            FIELD_AVATAR_BASE64 to avatarBase64
        )
        // role может быть null — если пользователь снял выбор,
        // пишем null явно, чтобы затереть в Firestore
        data[FIELD_ROLE] = role?.name ?: ""

        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(data, SetOptions.merge())
            .await()

        _currentUser.value = _currentUser.value?.copy(
            name = name,
            description = description,
            avatarBase64 = avatarBase64,
            role = role
        )
    }

    /**
     * Очистка стейта — вызывается SessionRepository при выходе.
     */
    fun clear() {
        _currentUser.value = null
    }

    private fun DocumentSnapshot.toUser(uid: String): User = User(
        uid = uid,
        email = getString(FIELD_EMAIL) ?: "",
        name = getString(FIELD_NAME) ?: "",
        description = getString(FIELD_DESCRIPTION) ?: "",
        avatarBase64 = getString(FIELD_AVATAR_BASE64) ?: "",
        role = Role.fromKey(getString(FIELD_ROLE)),
        onboardingCompleted = getBoolean(FIELD_ONBOARDING_COMPLETED) ?: false
    )

    private companion object {
        const val COLLECTION_USERS = "users"
        const val FIELD_EMAIL = "email"
        const val FIELD_NAME = "name"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_AVATAR_BASE64 = "avatarBase64"
        const val FIELD_ROLE = "role"
        const val FIELD_ONBOARDING_COMPLETED = "onboardingCompleted"
    }
    override suspend fun ensureUserExists(uid: String, email: String): Result<User> =
        runCatchingCancellable {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            val user = if (doc.exists()) {
                doc.toUser(uid)
            } else {
                val data = mapOf(
                    FIELD_EMAIL to email,
                    FIELD_NAME to "",
                    FIELD_DESCRIPTION to "",
                    FIELD_AVATAR_BASE64 to "",
                    FIELD_ONBOARDING_COMPLETED to false
                )
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .set(data)
                    .await()

                User(
                    uid = uid,
                    email = email,
                    name = "",
                    onboardingCompleted = false
                )
            }

            _currentUser.value = user
            user
        }
}