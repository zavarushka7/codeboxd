package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    // ── Отзывы ──

    override fun observeReviewsForUser(userId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(REVIEWS_PAGE_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val reviews = snapshot?.documents?.mapNotNull { it.toReviewOrNull() }.orEmpty()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    override fun observeReviewsForItem(itemId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(REVIEWS_PAGE_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val reviews = snapshot?.documents?.mapNotNull { it.toReviewOrNull() }.orEmpty()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getReview(userId: String, itemId: String): Review? =
        runCatchingCancellable {
            val doc = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewDocId(userId, itemId))
                .get()
                .await()
            if (!doc.exists()) null else doc.toReview(userId, itemId)
        }.getOrNull()

    override suspend fun saveReview(review: Review): Result<Unit> = runCatchingCancellable {
        val reviewRef = firestore.collection(COLLECTION_REVIEWS).document(review.reviewId)
        val itemRef = firestore.collection(COLLECTION_ITEMS).document(review.itemId)

        firestore.runTransaction { tx ->
            val existing = tx.get(reviewRef)
            val itemSnap = tx.get(itemRef)
            if (!itemSnap.exists()) {
                throw IllegalStateException("Айтем не найден")
            }

            val prevRating: Int? = if (existing.exists()) {
                existing.getLong("rating")?.toInt()
            } else {
                null
            }

            val createdAt = if (existing.exists())
                existing.getTimestamp("createdAt") ?: Timestamp.now()
            else Timestamp.now()

            val prevSum = itemSnap.getLong("ratingSum") ?: 0L
            val prevCount = itemSnap.getLong("ratingCount") ?: 0L

            val newSum: Long = if (prevRating == null) {
                prevSum + review.rating.toLong()
            } else {
                prevSum + (review.rating - prevRating).toLong()
            }
            val newCount: Long = if (prevRating == null) prevCount + 1L else prevCount
            val newAvg: Double = if (newCount <= 0L) 0.0 else newSum.toDouble() / newCount

            val data = mapOf(
                "userId" to review.userId,
                "itemId" to review.itemId,
                "rating" to review.rating,
                "comment" to review.comment,
                "authorName" to review.authorName,
                "authorAvatarBase64" to review.authorAvatarBase64,
                "itemName" to review.itemName,
                "itemSymbol" to review.itemSymbol,
                "itemIconKey" to review.itemIconKey,
                "createdAt" to createdAt,
                "updatedAt" to Timestamp.now()
            )
            tx.set(reviewRef, data, SetOptions.merge())
            tx.set(
                itemRef,
                mapOf(
                    "ratingSum" to newSum.coerceAtLeast(0L),
                    "ratingCount" to newCount.coerceAtLeast(0L),
                    "averageRating" to newAvg
                ),
                SetOptions.merge()
            )
        }.await()
        Unit
    }

    override suspend fun deleteReview(userId: String, itemId: String): Result<Unit> =
        runCatchingCancellable {
            val reviewRef = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewDocId(userId, itemId))
            val itemRef = firestore.collection(COLLECTION_ITEMS).document(itemId)

            firestore.runTransaction { tx ->
                val existing = tx.get(reviewRef)
                if (!existing.exists()) return@runTransaction

                val itemSnap = tx.get(itemRef)
                if (!itemSnap.exists()) {
                    tx.delete(reviewRef)
                    return@runTransaction
                }

                val prevRating: Int? = existing.getLong("rating")?.toInt()
                val prevSum = itemSnap.getLong("ratingSum") ?: 0L
                val prevCount = itemSnap.getLong("ratingCount") ?: 0L

                val newSum: Long
                val newCount: Long
                if (prevRating != null) {
                    newSum = (prevSum - prevRating.toLong()).coerceAtLeast(0L)
                    newCount = (prevCount - 1L).coerceAtLeast(0L)
                } else {
                    newSum = prevSum
                    newCount = prevCount
                }
                val newAvg = if (newCount <= 0L) 0.0 else newSum.toDouble() / newCount

                tx.delete(reviewRef)
                tx.set(
                    itemRef,
                    mapOf(
                        "ratingSum" to newSum,
                        "ratingCount" to newCount,
                        "averageRating" to newAvg
                    ),
                    SetOptions.merge()
                )
            }.await()
            Unit
        }

    // ── Реакции ──

    override fun observeMyReaction(reviewId: String, userId: String): Flow<Reaction?> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION_REACTIONS)
                .document(reactionDocId(userId, reviewId))
                .addSnapshotListener { doc, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    if (isClosedForSend) return@addSnapshotListener
                    val reaction = doc?.getString("type")?.let {
                        runCatching { Reaction.valueOf(it) }.getOrNull()
                    }
                    trySend(reaction)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun react(
        reviewId: String,
        userId: String,
        reaction: Reaction
    ): Result<Unit> = runCatchingCancellable {
        val reviewRef = firestore.collection(COLLECTION_REVIEWS).document(reviewId)
        val reactionRef = firestore.collection(COLLECTION_REACTIONS)
            .document(reactionDocId(userId, reviewId))

        firestore.runTransaction { tx ->
            val reviewSnap = tx.get(reviewRef)
            if (!reviewSnap.exists()) {
                throw IllegalStateException("Отзыв не найден")
            }
            val reactionSnap = tx.get(reactionRef)
            val previous = reactionSnap.getString("type")?.let {
                runCatching { Reaction.valueOf(it) }.getOrNull()
            }
            if (previous == reaction) return@runTransaction

            var likes = reviewSnap.getLong("likes")?.toInt() ?: 0
            var dislikes = reviewSnap.getLong("dislikes")?.toInt() ?: 0

            when (previous) {
                Reaction.LIKE -> likes = (likes - 1).coerceAtLeast(0)
                Reaction.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
                null -> Unit
            }
            when (reaction) {
                Reaction.LIKE -> likes += 1
                Reaction.DISLIKE -> dislikes += 1
            }

            tx.update(reviewRef, mapOf("likes" to likes, "dislikes" to dislikes))
            tx.set(
                reactionRef,
                mapOf(
                    "userId" to userId,
                    "reviewId" to reviewId,
                    "type" to reaction.name
                )
            )
        }.await()
    }

    override suspend fun removeReaction(reviewId: String, userId: String): Result<Unit> =
        runCatchingCancellable {
            val reviewRef = firestore.collection(COLLECTION_REVIEWS).document(reviewId)
            val reactionRef = firestore.collection(COLLECTION_REACTIONS)
                .document(reactionDocId(userId, reviewId))

            firestore.runTransaction { tx ->
                val reactionSnap = tx.get(reactionRef)
                val previous = reactionSnap.getString("type")?.let {
                    runCatching { Reaction.valueOf(it) }.getOrNull()
                } ?: return@runTransaction

                val reviewSnap = tx.get(reviewRef)
                if (!reviewSnap.exists()) return@runTransaction

                var likes = reviewSnap.getLong("likes")?.toInt() ?: 0
                var dislikes = reviewSnap.getLong("dislikes")?.toInt() ?: 0

                when (previous) {
                    Reaction.LIKE -> likes = (likes - 1).coerceAtLeast(0)
                    Reaction.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
                }
                tx.update(reviewRef, mapOf("likes" to likes, "dislikes" to dislikes))
                tx.delete(reactionRef)
            }.await()
        }

    override fun observeMyReactionsForReviews(
        reviewIds: List<String>,
        userId: String
    ): Flow<Map<String, Reaction?>> = callbackFlow {
        if (reviewIds.isEmpty()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val chunks = reviewIds.chunked(REACTIONS_WHERE_IN_LIMIT)
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val perChunk = ConcurrentHashMap<String, Map<String, Reaction?>>()

        fun emitMerged() {
            if (isClosedForSend) return
            val merged = HashMap<String, Reaction?>()
            perChunk.values.forEach { merged.putAll(it) }
            trySend(merged)
        }

        chunks.forEach { chunk ->
            val chunkKey = chunk.first()
            val reg = firestore.collection(COLLECTION_REACTIONS)
                .whereEqualTo("userId", userId)
                .whereIn("reviewId", chunk)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    if (isClosedForSend) return@addSnapshotListener

                    val chunkMap = HashMap<String, Reaction?>()
                    snapshot?.documents?.forEach { doc ->
                        val rid = doc.getString("reviewId") ?: return@forEach
                        chunkMap[rid] = doc.getString("type")?.let {
                            runCatching { Reaction.valueOf(it) }.getOrNull()
                        }
                    }
                    perChunk[chunkKey] = chunkMap
                    emitMerged()
                }
            listeners += reg
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    // ── Комментарии ──

    override suspend fun loadComments(reviewId: String): Result<List<ReviewComment>> =
        runCatchingCancellable {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .limit(ItemConstraints.COMMENTS_PAGE_SIZE)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toCommentOrNull() }
        }

    override suspend fun loadMoreComments(
        reviewId: String,
        lastCreatedAtMillis: Long,
        lastId: String
    ): Result<List<ReviewComment>> = runCatchingCancellable {
        val lastSnap = runCatching {
            firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_COMMENTS)
                .document(lastId)
                .get()
                .await()
        }.getOrNull()

        val cursorTimestamp = lastSnap?.getTimestamp("createdAt")
            ?: Timestamp(
                lastCreatedAtMillis / 1000,
                ((lastCreatedAtMillis % 1000) * 1_000_000).toInt()
            )

        val snapshot = firestore.collection(COLLECTION_REVIEWS)
            .document(reviewId)
            .collection(COLLECTION_COMMENTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .startAfter(cursorTimestamp, lastId)
            .limit(ItemConstraints.COMMENTS_PAGE_SIZE)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toCommentOrNull() }.reversed()
    }

    override fun observeComments(reviewId: String): Flow<List<ReviewComment>> = callbackFlow {
        // Запрашиваем PAGE_SIZE + 1, чтобы понять, есть ли ещё комментарии.
        // ViewModel обрежет до PAGE_SIZE и выставит canLoadMore корректно.
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .document(reviewId)
            .collection(COLLECTION_COMMENTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(ItemConstraints.COMMENTS_PAGE_SIZE + 1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val comments = snapshot?.documents?.mapNotNull { it.toCommentOrNull() }
                    .orEmpty()
                    .reversed()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: ReviewComment): Result<Unit> =
        runCatchingCancellable {
            val data = mapOf(
                "reviewId" to comment.reviewId,
                "authorId" to comment.authorId,
                "authorName" to comment.authorName,
                "authorAvatarBase64" to comment.authorAvatarBase64,
                "text" to comment.text,
                "createdAt" to Timestamp.now()
            )
            firestore.collection(COLLECTION_REVIEWS)
                .document(comment.reviewId)
                .collection(COLLECTION_COMMENTS)
                .add(data)
                .await()
            Unit
        }

    override suspend fun deleteComment(reviewId: String, commentId: String): Result<Unit> =
        runCatchingCancellable {
            firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .await()
        }

    // ── Вспомогательное ──

    private fun DocumentSnapshot.toReviewOrNull(): Review? = runCatching {
        toReview(getString("userId") ?: "", getString("itemId") ?: "")
    }.getOrNull()

    private fun DocumentSnapshot.toReview(userId: String, itemId: String): Review = Review(
        userId = userId,
        itemId = itemId,
        rating = getLong("rating")?.toInt() ?: 0,
        comment = getString("comment") ?: "",
        likes = getLong("likes")?.toInt() ?: 0,
        dislikes = getLong("dislikes")?.toInt() ?: 0,
        authorName = getString("authorName") ?: "",
        authorAvatarBase64 = getString("authorAvatarBase64") ?: "",
        itemName = getString("itemName") ?: "",
        itemSymbol = getString("itemSymbol"),
        itemIconKey = getString("itemIconKey"),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    )

    private fun DocumentSnapshot.toCommentOrNull(): ReviewComment? = runCatching {
        ReviewComment(
            id = id,
            reviewId = getString("reviewId") ?: "",
            authorId = getString("authorId"),
            authorName = getString("authorName") ?: "",
            authorAvatarBase64 = getString("authorAvatarBase64") ?: "",
            text = getString("text") ?: "",
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }.getOrNull()

    private fun reviewDocId(userId: String, itemId: String): String = "$userId|$itemId"

    private fun reactionDocId(userId: String, reviewId: String): String = "$userId|$reviewId"

    private companion object {
        const val COLLECTION_REVIEWS = "reviews"
        const val COLLECTION_ITEMS = "items"
        const val COLLECTION_COMMENTS = "comments"
        const val COLLECTION_REACTIONS = "reactions"
        const val REVIEWS_PAGE_LIMIT = 50L
        const val REACTIONS_WHERE_IN_LIMIT = 30
    }
}