package ru.savushkina.core.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.model.Item
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ItemRepository {

    override fun observeItems(): Flow<List<Item>> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toItemOrNull() }.getOrNull()
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getItemById(itemId: String): Item? = runCatchingCancellable {
        val doc = firestore.collection(COLLECTION).document(itemId).get().await()
        if (!doc.exists()) null else doc.toItemOrNull()
    }.getOrNull()

    override suspend fun refreshItems(): Result<Unit> = runCatchingCancellable {
        firestore.collection(COLLECTION).get().await()
        Unit
    }

    override suspend fun searchByName(query: String): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION).get().await()
        val all: List<Item> = snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toItemOrNull() }.getOrNull()
        }
        val q = query.trim().lowercase()
        if (q.isBlank()) emptyList()
        else all.filter { it.name.lowercase().contains(q) }
    }.getOrDefault(emptyList())

    override suspend fun getTypes(): List<String> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION).get().await()
        snapshot.documents
            .mapNotNull { it.getString("type") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.getOrDefault(emptyList())

    override suspend fun getItemsByType(type: String): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION)
            .whereEqualTo("type", type)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toItemOrNull() }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun DocumentSnapshot.toItemOrNull(): Item? = runCatching {
        Item(
            id = getString("id") ?: id,
            name = getString("name") ?: "",
            type = getString("type") ?: "",
            description = getString("description") ?: "",
            symbol = getString("symbol"),
            iconKey = getString("iconKey"),
            averageRating = getDouble("averageRating") ?: 0.0
        )
    }.getOrNull()
    override fun observeItemById(itemId: String): Flow<Item?> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .document(itemId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val item = snapshot?.let { doc ->
                    if (doc.exists()) runCatching { doc.toItemOrNull() }.getOrNull()
                    else null
                }
                trySend(item)
            }
        awaitClose { listener.remove() }
    }
    override suspend fun getTopRated(): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION).get().await()
        snapshot.documents
            .mapNotNull { doc -> runCatching { doc.toItemOrNull() }.getOrNull() }
            .sortedByDescending { it.averageRating }
    }.getOrDefault(emptyList())

    override suspend fun getLowestRated(): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION).get().await()
        snapshot.documents
            .mapNotNull { doc -> runCatching { doc.toItemOrNull() }.getOrNull() }
            .sortedBy { it.averageRating }
    }.getOrDefault(emptyList())

    private companion object {
        const val COLLECTION = "items"
    }
}