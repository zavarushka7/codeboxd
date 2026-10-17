package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.model.UserCollection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CollectionRepository {

    override fun observeCollections(userId: String): Flow<List<UserCollection>> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { it.toCollectionOrNull() }
                        ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }

    override fun observeCollection(collectionId: String): Flow<UserCollection?> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION)
                .document(collectionId)
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(doc?.takeIf { it.exists() }?.toCollectionOrNull())
                }
            awaitClose { listener.remove() }
        }

    override suspend fun createCollection(
        collection: UserCollection
    ): Result<String> = runCatchingCancellable {
        val data = mapOf(
            "userId" to collection.userId,
            "name" to collection.name,
            "itemIds" to collection.itemIds,
            "isFixedSize" to collection.isFixedSize,
            "maxSize" to collection.maxSize,
            "allowedItemType" to collection.allowedItemType,
            "createdAt" to Timestamp.now()
        )
        val ref = firestore.collection(COLLECTION).add(data).await()
        ref.id
    }

    override suspend fun addItem(collectionId: String, itemId: String): Result<Unit> =
        runCatchingCancellable {
            val docRef = firestore.collection(COLLECTION).document(collectionId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                val isFixed = snapshot.getBoolean("isFixedSize") ?: false
                if (isFixed) {
                    throw IllegalStateException("Массив нельзя редактировать")
                }
                val current = (snapshot.get("itemIds") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList()
                if (current.contains(itemId)) {
                    throw IllegalStateException("Айтем уже добавлен")
                }
                tx.update(docRef, "itemIds", current + itemId)
            }.await()
            Unit
        }

    override suspend fun removeItem(collectionId: String, itemId: String): Result<Unit> =
        runCatchingCancellable {
            val docRef = firestore.collection(COLLECTION).document(collectionId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                val isFixed = snapshot.getBoolean("isFixedSize") ?: false
                if (isFixed) {
                    throw IllegalStateException("Массив нельзя редактировать")
                }
                val current = (snapshot.get("itemIds") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList()
                tx.update(docRef, "itemIds", current.filterNot { it == itemId })
            }.await()
            Unit
        }

    override suspend fun rename(collectionId: String, newName: String): Result<Unit> =
        runCatchingCancellable {
            firestore.collection(COLLECTION)
                .document(collectionId)
                .update("name", newName)
                .await()
            Unit
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> =
        runCatchingCancellable {
            firestore.collection(COLLECTION)
                .document(collectionId)
                .delete()
                .await()
            Unit
        }

    private fun DocumentSnapshot.toCollectionOrNull(): UserCollection? = runCatching {
        UserCollection(
            id = id,
            userId = getString("userId") ?: "",
            name = getString("name") ?: "",
            itemIds = (get("itemIds") as? List<*>)?.mapNotNull { it as? String }
                ?: emptyList(),
            isFixedSize = getBoolean("isFixedSize") ?: false,
            maxSize = getLong("maxSize")?.toInt() ?: 0,
            allowedItemType = getString("allowedItemType"),
            createdAt = getTimestamp("createdAt") ?: Timestamp.now()
        )
    }.getOrNull()

    private companion object {
        const val COLLECTION = "collections"
    }
}