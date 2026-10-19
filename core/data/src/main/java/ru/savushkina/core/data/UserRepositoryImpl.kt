package ru.savushkina.core.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.data.di.ApplicationScope
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    authRepository: AuthRepository,
    @ApplicationScope private val appScope: CoroutineScope
) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _profileLoading = MutableStateFlow(false)
    override val profileLoading: StateFlow<Boolean> = _profileLoading.asStateFlow()

    private val _profileError = MutableStateFlow<Throwable?>(null)
    override val profileError: StateFlow<Throwable?> = _profileError.asStateFlow()

    init {
        appScope.launch {
            authRepository.authState.collect { authUser ->
                if (authUser == null) {
                    _currentUser.value = null
                    _profileError.value = null
                    _profileLoading.value = false
                } else {
                    refreshInternal(authUser.uid, authUser.email.orEmpty())
                }
            }
        }
    }

    private suspend fun refreshInternal(uid: String, email: String) {
        _profileLoading.value = true
        _profileError.value = null
        try {
            runCatchingCancellable {
                val user = getOrCreate(uid, email)
                    ?: throw IllegalStateException("Профиль не найден")
                _currentUser.value = user
            }.onFailure { e ->
                _profileError.value = e
            }
        } finally {
            _profileLoading.value = false
        }
    }

    override suspend fun refresh(uid: String): Result<User> = runCatchingCancellable {
        _profileLoading.value = true
        _profileError.value = null
        try {
            val email = auth.currentUser?.email.orEmpty()
            val user = getOrCreate(uid, email)
                ?: throw IllegalStateException("Профиль не найден")
            _currentUser.value = user
            user
        } finally {
            _profileLoading.value = false
        }
    }

    override suspend fun getUser(uid: String): User? = runCatchingCancellable {
        val doc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
        if (!doc.exists()) null else doc.toUser(uid)
    }.getOrNull()

    private suspend fun getOrCreate(uid: String, email: String): User? {
        val docRef = firestore.collection(COLLECTION_USERS).document(uid)
        return firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (snap.exists()) {
                snap.toUser(uid)
            } else {
                val resolvedEmail = email.ifBlank { auth.currentUser?.email.orEmpty() }
                val data = mapOf(
                    FIELD_EMAIL to resolvedEmail,
                    FIELD_NAME to "",
                    FIELD_DESCRIPTION to "",
                    FIELD_AVATAR_BASE64 to "",
                    FIELD_ONBOARDING_COMPLETED to false
                )
                tx.set(docRef, data)
                User(uid = uid, email = resolvedEmail, name = "", onboardingCompleted = false)
            }
        }.await()
    }

    override suspend fun ensureUserExists(uid: String, email: String): Result<User> =
        runCatchingCancellable {
            val user = getOrCreate(uid, email)
                ?: throw IllegalStateException("Не удалось создать профиль")
            _currentUser.value = user
            user
        }

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
        ) ?: User(uid = uid, email = email, name = name, role = role, onboardingCompleted = true)
    }

    override suspend fun saveProfile(
        uid: String,
        name: String,
        description: String,
        avatarBase64: String,
        role: Role?
    ): Result<Unit> = runCatchingCancellable {
        val data = buildMap<String, Any> {
            put(FIELD_NAME, name)
            put(FIELD_DESCRIPTION, description)
            put(FIELD_AVATAR_BASE64, avatarBase64)
            if (role != null) {
                put(FIELD_ROLE, role.name)
            } else {
                put(FIELD_ROLE, FieldValue.delete())
            }
        }
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
}