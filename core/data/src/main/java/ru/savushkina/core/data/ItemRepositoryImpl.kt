package ru.savushkina.core.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.Page
import ru.savushkina.core.model.Item
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ItemRepository {

    override fun observeItems(limit: Long): Flow<List<Item>> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .orderBy("name", Query.Direction.ASCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (isClosedForSend) return@addSnapshotListener
                trySend(snapshot?.documents?.mapNotNull { it.toItemOrNull() }.orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override fun observeItemById(itemId: String): Flow<Item?> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .document(itemId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (isClosedForSend) return@addSnapshotListener
                trySend(snapshot?.takeIf { it.exists() }?.toItemOrNull())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getItemById(itemId: String): Item? = runCatchingCancellable {
        val doc = firestore.collection(COLLECTION).document(itemId).get().await()
        if (!doc.exists()) null else doc.toItemOrNull()
    }.getOrNull()

    override suspend fun getItemsByIds(ids: List<String>): List<Item> {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return emptyList()
        return runCatchingCancellable {
            val byId = mutableMapOf<String, Item>()
            supervisorScope {
                distinctIds.chunked(FIRESTORE_IN_LIMIT).map { chunk ->
                    async {
                        runCatchingCancellable {
                            firestore.collection(COLLECTION)
                                .whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .await()
                                .documents
                                .mapNotNull { it.toItemOrNull() }
                        }.onFailure { e ->
                            Log.w(TAG, "getItemsByIds: chunk failed (${chunk.size} ids)", e)
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten().forEach { byId[it.id] = it }
            }
            distinctIds.mapNotNull { byId[it] }
        }.getOrDefault(emptyList())
    }

    override suspend fun searchByName(query: String, limit: Long): List<Item> =
        runCatchingCancellable {
            val q = query.trim().lowercase()
            if (q.isBlank()) return@runCatchingCancellable emptyList()

            val snapshot = firestore.collection(COLLECTION)
                .orderBy("name")
                .limit(SEARCH_FETCH_LIMIT)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toItemOrNull() }
                .filter { it.name.lowercase().contains(q) }
                .take(limit.toInt())
        }.getOrDefault(emptyList())

    override suspend fun getTypes(): List<String> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION)
            .get()
            .await()
        snapshot.documents
            .mapNotNull { it.getString("type") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.getOrDefault(emptyList())

    override suspend fun getItemsByType(type: String, limit: Long): List<Item> =
        runCatchingCancellable {
            if (type.isBlank()) return@runCatchingCancellable emptyList()
            val snapshot = firestore.collection(COLLECTION)
                .whereEqualTo("type", type)
                .limit(TYPE_FETCH_LIMIT)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { it.toItemOrNull() }
                .sortedBy { it.name.lowercase() }
                .take(limit.toInt())
        }.getOrDefault(emptyList())

    /**
     * Топ по рейтингу.
     *
     * Раньше был Firestore-запрос:
     *   .whereGreaterThan("ratingCount", 0L).orderBy("averageRating", DESC)
     * — он возвращал пусто, если у items нет поля ratingCount (созданы
     * вручную через консоль) или если не задеплоен составной индекс.
     *
     * Теперь: забираем все items (limit 200), сортируем на клиенте по
     * эффективному рейтингу (averageRating или ratingSum/ratingCount),
     * фильтруем > 0. Не требует индексов и миграции данных.
     * Для продакшена — лучше проставить averageRating всем items и
     * оставить Firestore-запрос (см. комментарий в классе).
     */
    override suspend fun getTopRated(limit: Long): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION)
            .limit(RATING_FETCH_LIMIT)
            .get()
            .await()
        snapshot.documents
            .mapNotNull { it.toItemOrNull() }
            .filter { it.averageRating > 0.0 }
            .sortedByDescending { it.averageRating }
            .take(limit.toInt())
    }.onFailure { Log.w(TAG, "getTopRated failed", it) }.getOrDefault(emptyList())

    override suspend fun getLowestRated(limit: Long): List<Item> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION)
            .limit(RATING_FETCH_LIMIT)
            .get()
            .await()
        snapshot.documents
            .mapNotNull { it.toItemOrNull() }
            .filter { it.averageRating > 0.0 }
            .sortedBy { it.averageRating }
            .take(limit.toInt())
    }.onFailure { Log.w(TAG, "getLowestRated failed", it) }.getOrDefault(emptyList())

    override suspend fun loadItemsPage(
        limit: Long,
        lastDocId: String?
    ): Result<Page<Item>> = runCatchingCancellable {
        var query: Query = firestore.collection(COLLECTION)
            .orderBy("name")
            .limit(limit)

        if (!lastDocId.isNullOrBlank()) {
            val lastDoc = firestore.collection(COLLECTION)
                .document(lastDocId)
                .get()
                .await()
            if (lastDoc.exists()) query = query.startAfter(lastDoc)
        }

        val docs = query.get().await().documents
        val items = docs.mapNotNull { it.toItemOrNull() }
        Page(items = items, nextCursor = docs.lastOrNull()?.id)
    }

    private fun DocumentSnapshot.toItemOrNull(): Item? = runCatching {
        // averageRating может отсутствовать в старых документах — считаем
        // на клиенте из ratingSum/ratingCount, чтобы не терять рейтинг.
        val storedAverage = getDouble("averageRating")
        val computedAverage = run {
            val sum = getLong("ratingSum") ?: 0L
            val count = getLong("ratingCount") ?: 0L
            if (count > 0L) sum.toDouble() / count else 0.0
        }
        Item(
            id = id,
            name = getString("name") ?: "",
            type = (getString("type") ?: "").trim(),
            description = getString("description") ?: "",
            symbol = getString("symbol"),
            iconKey = getString("iconKey"),
            averageRating = storedAverage ?: computedAverage
        )
    }.getOrNull()

    private companion object {
        const val TAG = "ItemRepositoryImpl"
        const val COLLECTION = "items"
        const val FIRESTORE_IN_LIMIT = 30
        const val SEARCH_FETCH_LIMIT = 200L
        const val TYPE_FETCH_LIMIT = 200L
        const val RATING_FETCH_LIMIT = 200L
    }
}