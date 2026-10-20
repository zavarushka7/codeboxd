package ru.savushkina.core.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FeedRepository
import ru.savushkina.core.model.FeedItem
import ru.savushkina.core.model.FeedItemType
import ru.savushkina.core.model.Reaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FeedRepository {

    override fun observeFeed(userId: String, limit: Long): Flow<List<FeedItem>> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION_FEEDS)
                .document(userId)
                .collection(COLLECTION_ITEMS)
                .orderBy("timestampMillis", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    if (isClosedForSend) return@addSnapshotListener
                    val items = snapshot?.documents
                        ?.mapNotNull { it.toFeedItemOrNull() }
                        .orEmpty()
                    trySend(items)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun loadMore(
        userId: String,
        lastTimestampMillis: Long,
        limit: Long
    ): Result<List<FeedItem>> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION_FEEDS)
            .document(userId)
            .collection(COLLECTION_ITEMS)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .startAfter(lastTimestampMillis)
            .limit(limit)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toFeedItemOrNull() }
    }

    private fun DocumentSnapshot.toFeedItemOrNull(): FeedItem? = runCatching {
        val typeRaw = getString("type") ?: return@runCatching null
        val type = FeedItemType.valueOf(typeRaw)
        FeedItem(
            id = id,
            ownerUid = getString("ownerUid") ?: "",
            type = type,
            actorUid = getString("actorUid") ?: "",
            actorName = getString("actorName") ?: "",
            actorAvatarBase64 = getString("actorAvatarBase64") ?: "",
            reviewId = getString("reviewId") ?: "",
            reviewComment = getString("reviewComment") ?: "",
            itemName = getString("itemName") ?: "",
            rating = getLong("rating")?.toInt() ?: 0,
            commentText = getString("commentText") ?: "",
            reaction = getString("reaction")?.let {
                runCatching { Reaction.valueOf(it) }.getOrNull()
            },
            timestampMillis = getLong("timestampMillis") ?: 0L
        )
    }.getOrNull()

    private companion object {
        const val COLLECTION_FEEDS = "feeds"
        const val COLLECTION_ITEMS = "items"
    }
}