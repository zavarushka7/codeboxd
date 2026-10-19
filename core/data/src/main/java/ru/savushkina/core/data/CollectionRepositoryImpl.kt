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
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (isClosedForSend) return@addSnapshotListener
                    val list = snapshot?.documents
                        ?.mapNotNull { it.toCollectionOrNull() }
                        .orEmpty()
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
                    if (isClosedForSend) return@addSnapshotListener
                    trySend(doc?.takeIf { it.exists() }?.toCollectionOrNull())
                }
            awaitClose { listener.remove() }
        }

    override suspend fun createCollection(
        collection: UserCollection
    ): Result<String> = runCatchingCancellable {
        require(collection.name.isNotBlank()) { "Имя коллекции не может быть пустым" }
        require(collection.name.length <= UserCollection.NAME_MAX) {
            "Имя коллекции слишком длинное (макс. ${UserCollection.NAME_MAX})"
        }
        if (collection.isFixedSize) {
            require(collection.itemIds.size in UserCollection.MIN_SIZE..UserCollection.MAX_SIZE_LIMIT) {
                "Размер массива должен быть от ${UserCollection.MIN_SIZE} до ${UserCollection.MAX_SIZE_LIMIT}"
            }
            require(!collection.allowedItemType.isNullOrBlank()) {
                "Для массива нужно указать тип айтемов"
            }
            require(collection.maxSize == collection.itemIds.size) {
                "maxSize массива должен совпадать с количеством itemIds"
            }
        }

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
            val collectionRef = firestore.collection(COLLECTION).document(collectionId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(collectionRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Коллекция не найдена")
                }
                val isFixed = snapshot.getBoolean("isFixedSize") ?: false
                if (isFixed) {
                    throw IllegalStateException("Массив нельзя редактировать")
                }
                val current = (snapshot.get("itemIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (current.contains(itemId)) {
                    throw IllegalStateException("Айтем уже добавлен")
                }
                tx.update(collectionRef, "itemIds", current + itemId)
            }.await()
            Unit
        }

    override suspend fun removeItem(collectionId: String, itemId: String): Result<Unit> =
        runCatchingCancellable {
            val docRef = firestore.collection(COLLECTION).document(collectionId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Коллекция не найдена")
                }
                val isFixed = snapshot.getBoolean("isFixedSize") ?: false
                if (isFixed) {
                    throw IllegalStateException("Массив нельзя редактировать")
                }
                val current = (snapshot.get("itemIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                tx.update(docRef, "itemIds", current.filterNot { it == itemId })
            }.await()
            Unit
        }

    override suspend fun rename(collectionId: String, newName: String): Result<Unit> =
        runCatchingCancellable {
            require(newName.isNotBlank()) { "Имя не может быть пустым" }
            require(newName.length <= UserCollection.NAME_MAX) {
                "Имя слишком длинное (макс. ${UserCollection.NAME_MAX})"
            }
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
            itemIds = (get("itemIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            isFixedSize = getBoolean("isFixedSize") ?: false,
            maxSize = getLong("maxSize")?.toInt() ?: 0,
            allowedItemType = getString("allowedItemType"),
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }.getOrNull()

    private companion object {
        const val COLLECTION = "collections"
    }
}