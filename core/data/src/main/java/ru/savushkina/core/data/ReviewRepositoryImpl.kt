package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AchievementRepository
import ru.savushkina.core.domain.FeedWriter
import ru.savushkina.core.domain.Page
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.model.FeedItemType
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val feedWriter: FeedWriter,
    private val achievementRepository: AchievementRepository
) : ReviewRepository {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Отзывы ──

    /**
     * Live-поток отзывов пользователя.
     *
     * ВАЖНО: слушаем с MetadataChanges.INCLUDE и НЕ эмитим пустой кэш-снапшот.
     *
     * Причина: Firestore при подписке сначала отдаёт локальный кэш. Если в кэше
     * для этого запроса данных ещё нет, приходит пустой snapshot с
     * metadata.isFromCache == true. Если его эмитить, UI успевает показать
     * «// пока нет отзывов», а затем приходит серверный снапшот со списком —
     * получается визуальное мигание. Поэтому пустые кэш-снапшоты пропускаем,
     * а пустой серверный — эмитим (это реальный ответ «отзывов нет»).
     */
    override fun observeReviewsForUser(userId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(REVIEWS_PAGE_LIMIT)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (isClosedForSend) return@addSnapshotListener
                if (snapshot == null) return@addSnapshotListener

                val fromCache = snapshot.metadata.isFromCache
                val reviews = snapshot.documents.mapNotNull { it.toReviewOrNull() }

                // Пропускаем пустой кэш — ждём серверный снапшот.
                if (fromCache && reviews.isEmpty()) return@addSnapshotListener

                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    override fun observeReviewsForItem(
        itemId: String,
        limit: Long
    ): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                if (snapshot == null) return@addSnapshotListener

                val fromCache = snapshot.metadata.isFromCache
                val reviews = snapshot.documents.mapNotNull { it.toReviewOrNull() }

                // Та же защита: не эмитим пустой кэш, чтобы не мигать
                // «// пока нет отзывов» → список.
                if (fromCache && reviews.isEmpty()) return@addSnapshotListener

                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun loadMoreReviews(
        itemId: String,
        lastDocId: String,
        limit: Long
    ): Result<Page<Review>> = runCatchingCancellable {
        var query = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        if (lastDocId.isNotBlank()) {
            val lastDoc = firestore.collection(COLLECTION_REVIEWS)
                .document(lastDocId)
                .get()
                .await()
            if (lastDoc.exists()) query = query.startAfter(lastDoc)
        }

        val docs = query.get().await().documents
        val reviews = docs.mapNotNull { it.toReviewOrNull() }
        Page(items = reviews, nextCursor = docs.lastOrNull()?.id)
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

        val isNew = java.util.concurrent.atomic.AtomicBoolean(false)

        firestore.runTransaction { tx ->
            val existing = tx.get(reviewRef)
            isNew.set(!existing.exists())
            val itemSnap = tx.get(itemRef)
            if (!itemSnap.exists()) {
                throw IllegalStateException("Айтем не найден")
            }

            val prevRating: Int? = if (existing.exists()) {
                existing.getLong("rating")?.toInt()
            } else null

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

        if (isNew.get()) {
            feedWriter.fanOut(
                actorUid = review.userId,
                sourceId = review.reviewId,
                type = FeedItemType.FOLLOWING_REVIEW,
                payload = FeedWriter.FeedPayload(
                    actorName = review.authorName,
                    actorAvatarBase64 = review.authorAvatarBase64,
                    reviewId = review.reviewId,
                    reviewComment = review.comment,
                    itemName = review.itemName,
                    rating = review.rating
                )
            )
        }

        backgroundScope.launch {
            achievementRepository.checkAndNotifyLevelUps(review.userId)
        }
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

            backgroundScope.launch {
                achievementRepository.checkAndNotifyLevelUps(userId)
            }
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

    override fun observeReactionsForReview(
        reviewId: String
    ): Flow<List<ReviewRepository.ReactionEntry>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REACTIONS)
            .whereEqualTo("reviewId", reviewId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    val type = doc.getString("type")?.let {
                        runCatching { Reaction.valueOf(it) }.getOrNull()
                    } ?: return@mapNotNull null
                    ReviewRepository.ReactionEntry(
                        userId = uid,
                        reaction = type,
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }.orEmpty()
                trySend(entries)
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

        var ownerUid: String? = null
        var previousReaction: Reaction? = null

        firestore.runTransaction { tx ->
            val reviewSnap = tx.get(reviewRef)
            if (!reviewSnap.exists()) {
                throw IllegalStateException("Отзыв не найден")
            }
            ownerUid = reviewSnap.getString("userId")
            val reactionSnap = tx.get(reactionRef)
            previousReaction = reactionSnap.getString("type")?.let {
                runCatching { Reaction.valueOf(it) }.getOrNull()
            }
            if (previousReaction == reaction) return@runTransaction

            var likes = reviewSnap.getLong("likes")?.toInt() ?: 0
            var dislikes = reviewSnap.getLong("dislikes")?.toInt() ?: 0

            when (previousReaction) {
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
                    "type" to reaction.name,
                    "createdAt" to Timestamp.now()
                )
            )
        }.await()

        val owner = ownerUid
        if (owner != null && owner != userId) {
            val review = runCatching { getReviewByDocId(reviewId) }.getOrNull()
            val actor = runCatching {
                firestore.collection(COLLECTION_USERS).document(userId).get().await()
            }.getOrNull()
            feedWriter.fanOut(
                actorUid = userId,
                sourceId = "${reactionDocId(userId, reviewId)}_${reaction.name}",
                type = FeedItemType.REVIEW_REACTION,
                payload = FeedWriter.FeedPayload(
                    actorName = actor?.getString("name").orEmpty(),
                    actorAvatarBase64 = actor?.getString("avatarBase64").orEmpty(),
                    reviewId = reviewId,
                    reviewComment = review?.comment.orEmpty(),
                    itemName = review?.itemName.orEmpty(),
                    reaction = reaction
                ),
                extraOwnerUid = owner
            )
        }

        backgroundScope.launch {
            achievementRepository.checkAndNotifyLevelUps(userId)
            if (owner != null && owner != userId) {
                achievementRepository.checkAndNotifyLevelUps(owner)
            }
        }
        Unit
    }

    override suspend fun removeReaction(reviewId: String, userId: String): Result<Unit> =
        runCatchingCancellable {
            val reviewRef = firestore.collection(COLLECTION_REVIEWS).document(reviewId)
            val reactionRef = firestore.collection(COLLECTION_REACTIONS)
                .document(reactionDocId(userId, reviewId))

            var ownerUid: String? = null

            firestore.runTransaction { tx ->
                val reactionSnap = tx.get(reactionRef)
                val previous = reactionSnap.getString("type")?.let {
                    runCatching { Reaction.valueOf(it) }.getOrNull()
                } ?: return@runTransaction

                val reviewSnap = tx.get(reviewRef)
                if (!reviewSnap.exists()) return@runTransaction
                ownerUid = reviewSnap.getString("userId")

                var likes = reviewSnap.getLong("likes")?.toInt() ?: 0
                var dislikes = reviewSnap.getLong("dislikes")?.toInt() ?: 0

                when (previous) {
                    Reaction.LIKE -> likes = (likes - 1).coerceAtLeast(0)
                    Reaction.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
                }
                tx.update(reviewRef, mapOf("likes" to likes, "dislikes" to dislikes))
                tx.delete(reactionRef)
            }.await()

            backgroundScope.launch {
                achievementRepository.checkAndNotifyLevelUps(userId)
                val owner = ownerUid
                if (owner != null && owner != userId) {
                    achievementRepository.checkAndNotifyLevelUps(owner)
                }
            }
            Unit
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
            val ref = firestore.collection(COLLECTION_REVIEWS)
                .document(comment.reviewId)
                .collection(COLLECTION_COMMENTS)
                .add(data)
                .await()

            val review = runCatching { getReviewByDocId(comment.reviewId) }.getOrNull()
            val ownerUid = review?.userId
            if (!ownerUid.isNullOrBlank() && ownerUid != comment.authorId) {
                feedWriter.fanOut(
                    actorUid = comment.authorId.orEmpty(),
                    sourceId = ref.id,
                    type = FeedItemType.REVIEW_COMMENT,
                    payload = FeedWriter.FeedPayload(
                        actorName = comment.authorName,
                        actorAvatarBase64 = comment.authorAvatarBase64,
                        reviewId = comment.reviewId,
                        reviewComment = review?.comment.orEmpty(),
                        itemName = review?.itemName.orEmpty(),
                        commentText = comment.text
                    ),
                    extraOwnerUid = ownerUid
                )
            }
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

    private suspend fun getReviewByDocId(reviewId: String): Review? =
        runCatching {
            val doc = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .get()
                .await()
            if (!doc.exists()) null else doc.toReview(
                doc.getString("userId") ?: "",
                doc.getString("itemId") ?: ""
            )
        }.getOrNull()

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
        const val COLLECTION_USERS = "users"
        const val REVIEWS_PAGE_LIMIT = 50L
        const val REACTIONS_WHERE_IN_LIMIT = 30
    }
}