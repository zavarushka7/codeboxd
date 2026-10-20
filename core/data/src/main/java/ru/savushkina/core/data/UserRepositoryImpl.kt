package ru.savushkina.core.data

import android.util.LruCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

    private data class CachedUser(val user: User, val fetchedAt: Long)

    private val userCache = object : LruCache<String, CachedUser>(CACHE_MAX_ENTRIES) {}

    private fun cacheGet(uid: String): User? {
        val entry = userCache.get(uid) ?: return null
        if (System.currentTimeMillis() - entry.fetchedAt > CACHE_TTL_MS) {
            userCache.remove(uid)
            return null
        }
        return entry.user
    }

    private fun cachePut(uid: String, user: User) {
        userCache.put(uid, CachedUser(user, System.currentTimeMillis()))
    }

    private fun cacheInvalidate(uid: String) {
        userCache.remove(uid)
    }

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
                cachePut(uid, user)
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
            cacheInvalidate(uid)
            val email = auth.currentUser?.email.orEmpty()
            val user = getOrCreate(uid, email)
                ?: throw IllegalStateException("Профиль не найден")
            _currentUser.value = user
            cachePut(uid, user)
            user
        } finally {
            _profileLoading.value = false
        }
    }

    override suspend fun getUser(uid: String): User? {
        cacheGet(uid)?.let { return it }
        return runCatchingCancellable {
            val doc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
            if (!doc.exists()) null else doc.toUser(uid).also { cachePut(uid, it) }
        }.getOrNull()
    }

    override suspend fun getUsersByIds(ids: List<String>): List<User> {
        val distinctIds = ids.distinct().filter { it.isNotBlank() }
        if (distinctIds.isEmpty()) return emptyList()

        val result = mutableMapOf<String, User>()
        val missing = mutableListOf<String>()
        for (uid in distinctIds) {
            val cached = cacheGet(uid)
            if (cached != null) result[uid] = cached else missing.add(uid)
        }
        if (missing.isEmpty()) return distinctIds.mapNotNull { result[it] }

        runCatchingCancellable {
            supervisorScope {
                missing.chunked(FIRESTORE_IN_LIMIT).map { chunk ->
                    async {
                        runCatchingCancellable {
                            firestore.collection(COLLECTION_USERS)
                                .whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .await()
                                .documents
                                .mapNotNull { doc ->
                                    doc.toUser(doc.id).also { cachePut(doc.id, it) }
                                }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten().forEach { result[it.uid] = it }
            }
        }
        return distinctIds.mapNotNull { result[it] }
    }

    override suspend fun getUserByUsername(username: String): User? =
        runCatchingCancellable {
            val clean = username.trim().lowercase().removePrefix("@")
            if (clean.isBlank()) return@runCatchingCancellable null
            val snap = firestore.collection(COLLECTION_USERS)
                .whereEqualTo(FIELD_USERNAME, clean)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.let { doc ->
                doc.toUser(doc.id).also { cachePut(doc.id, it) }
            }
        }.getOrNull()

    override suspend fun searchUsersByUsernamePrefix(
        prefix: String,
        limit: Long
    ): List<User> = runCatchingCancellable {
        val clean = prefix.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) return@runCatchingCancellable emptyList()
        val snap = firestore.collection(COLLECTION_USERS)
            .orderBy(FIELD_USERNAME)
            .startAt(clean)
            .endAt(clean + "\uf8ff")
            .limit(limit)
            .get()
            .await()
        snap.documents.mapNotNull { doc ->
            doc.toUser(doc.id).also { cachePut(doc.id, it) }
        }
    }.getOrDefault(emptyList())

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> =
        runCatchingCancellable {
            val clean = username.trim().lowercase().removePrefix("@")
            val snap = firestore.collection(COLLECTION_USERS)
                .whereEqualTo(FIELD_USERNAME, clean)
                .limit(1)
                .get()
                .await()
            snap.isEmpty
        }

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
                    FIELD_USERNAME to "",
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
            cachePut(uid, user)
            user
        }

    override suspend fun saveOnboarding(
        uid: String,
        email: String,
        name: String,
        username: String,
        role: Role
    ): Result<Unit> = runCatchingCancellable {
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        require(cleanUsername.isNotBlank()) { "Username не может быть пустым" }

        val usernameRef = firestore.collection(COLLECTION_USERNAMES)
            .document(cleanUsername)
        firestore.runTransaction { tx ->
            val existing = tx.get(usernameRef)
            if (existing.exists()) {
                val owner = existing.getString("uid")
                if (owner != uid) {
                    throw IllegalStateException("Этот username уже занят")
                }
            } else {
                tx.set(
                    usernameRef,
                    mapOf(
                        "uid" to uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                )
            }
        }.await()

        val data = mapOf(
            FIELD_EMAIL to email,
            FIELD_NAME to name,
            FIELD_USERNAME to cleanUsername,
            FIELD_ROLE to role.name,
            FIELD_ONBOARDING_COMPLETED to true
        )
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(data, SetOptions.merge())
            .await()

        val updated = _currentUser.value?.copy(
            email = email,
            name = name,
            username = cleanUsername,
            role = role,
            onboardingCompleted = true
        ) ?: User(
            uid = uid, email = email, name = name, username = cleanUsername,
            role = role, onboardingCompleted = true
        )
        _currentUser.value = updated
        cachePut(uid, updated)
    }

    /**
     * Сохраняет профиль. Если [username] передан и отличается от текущего —
     * атомарно освобождает старый username и занимает новый в коллекции
     * `usernames`, обновляет `users/{uid}.username`.
     */
    override suspend fun saveProfile(
        uid: String,
        name: String,
        username: String?,
        description: String,
        avatarBase64: String,
        role: Role?
    ): Result<Unit> = runCatchingCancellable {
        val current = _currentUser.value
        val oldUsername = current?.username?.lowercase().orEmpty()
        val cleanNewUsername = username?.trim()?.lowercase()?.removePrefix("@")

        val userRef = firestore.collection(COLLECTION_USERS).document(uid)

        if (cleanNewUsername != null &&
            cleanNewUsername.isNotBlank() &&
            cleanNewUsername != oldUsername
        ) {
            // Смена username — атомарно.
            val newUsernameRef = firestore.collection(COLLECTION_USERNAMES)
                .document(cleanNewUsername)
            val oldUsernameRef = if (oldUsername.isNotBlank())
                firestore.collection(COLLECTION_USERNAMES).document(oldUsername)
            else null

            firestore.runTransaction { tx ->
                val newSnap = tx.get(newUsernameRef)
                if (newSnap.exists()) {
                    val owner = newSnap.getString("uid")
                    if (owner != uid) {
                        throw IllegalStateException("Этот username уже занят")
                    }
                }
                if (oldUsernameRef != null) {
                    tx.delete(oldUsernameRef)
                }
                tx.set(
                    newUsernameRef,
                    mapOf(
                        "uid" to uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                )
            }.await()
        }

        val data = buildMap<String, Any> {
            put(FIELD_NAME, name)
            put(FIELD_DESCRIPTION, description)
            put(FIELD_AVATAR_BASE64, avatarBase64)
            if (cleanNewUsername != null) {
                put(FIELD_USERNAME, cleanNewUsername)
            }
            if (role != null) {
                put(FIELD_ROLE, role.name)
            } else {
                put(FIELD_ROLE, FieldValue.delete())
            }
        }
        userRef.set(data, SetOptions.merge()).await()

        val updated = _currentUser.value?.copy(
            name = name,
            description = description,
            avatarBase64 = avatarBase64,
            role = role,
            username = cleanNewUsername ?: _currentUser.value?.username ?: ""
        )
        _currentUser.value = updated
        if (updated != null) cachePut(uid, updated)
    }

    private fun DocumentSnapshot.toUser(uid: String): User = User(
        uid = uid,
        email = getString(FIELD_EMAIL) ?: "",
        name = getString(FIELD_NAME) ?: "",
        username = getString(FIELD_USERNAME) ?: "",
        description = getString(FIELD_DESCRIPTION) ?: "",
        avatarBase64 = getString(FIELD_AVATAR_BASE64) ?: "",
        role = Role.fromKey(getString(FIELD_ROLE)),
        onboardingCompleted = getBoolean(FIELD_ONBOARDING_COMPLETED) ?: false
    )

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_USERNAMES = "usernames"
        const val FIELD_EMAIL = "email"
        const val FIELD_NAME = "name"
        const val FIELD_USERNAME = "username"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_AVATAR_BASE64 = "avatarBase64"
        const val FIELD_ROLE = "role"
        const val FIELD_ONBOARDING_COMPLETED = "onboardingCompleted"

        const val FIRESTORE_IN_LIMIT = 30
        const val CACHE_MAX_ENTRIES = 500
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}