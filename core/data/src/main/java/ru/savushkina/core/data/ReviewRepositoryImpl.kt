package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ReviewRepository {

    private val currentUserId: String?
        get() = authRepository.currentUserId

    // ── Отзывы ──

    override fun observeReviewsForUser(userId: String): Flow<List<Review>> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val reviews = snapshot?.documents?.mapNotNull { it.toReviewOrNull() }
                        ?: emptyList()
                    trySend(reviews)
                }
            awaitClose { listener.remove() }
        }

    override fun observeReviewsForItem(itemId: String): Flow<List<Review>> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("itemId", itemId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val reviews = snapshot?.documents?.mapNotNull { it.toReviewOrNull() }
                        ?: emptyList()
                    trySend(reviews)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun getReview(userId: String, itemId: String): Review? =
        runCatchingCancellable {
            val doc = firestore.collection(COLLECTION_REVIEWS)
                .document("${userId}_$itemId")
                .get()
                .await()
            if (!doc.exists()) null else doc.toReview(userId, itemId)
        }.getOrNull()

    override suspend fun saveReview(review: Review): Result<Unit> = runCatchingCancellable {
        val data = mapOf(
            "userId" to review.userId,
            "itemId" to review.itemId,
            "rating" to review.rating,
            "comment" to review.comment,
            "authorName" to review.authorName,
            "authorAvatarBase64" to review.authorAvatarBase64,
            "itemName" to review.itemName,
            "itemSymbol" to review.itemSymbol,
            "itemIconKey" to review.itemIconKey
        )
        firestore.collection(COLLECTION_REVIEWS)
            .document(review.reviewId)
            .set(data, SetOptions.merge())
            .await()
        recalculateAverageRating(review.itemId)
    }

    override suspend fun deleteReview(userId: String, itemId: String): Result<Unit> =
        runCatchingCancellable {
            firestore.collection(COLLECTION_REVIEWS)
                .document("${userId}_$itemId")
                .delete()
                .await()
            recalculateAverageRating(itemId)
        }

    // ── Реакции (привязаны к uid) ──

    override fun observeMyReaction(reviewId: String): Flow<Reaction?> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_REVIEWS)
            .document(reviewId)
            .collection(COLLECTION_REACTIONS)
            .document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reaction = doc?.getString("type")?.let {
                    runCatching { Reaction.valueOf(it) }.getOrNull()
                }
                trySend(reaction)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun react(reviewId: String, reaction: Reaction): Result<Unit> =
        runCatchingCancellable {
            val uid = currentUserId
                ?: throw IllegalStateException("Пользователь не авторизован")

            val reactionRef = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_REACTIONS)
                .document(uid)

            val existing = reactionRef.get().await()
            val previous = existing.getString("type")?.let {
                runCatching { Reaction.valueOf(it) }.getOrNull()
            }

            if (previous == reaction) return@runCatchingCancellable

            val updates = mutableMapOf<String, Any>()
            when (previous) {
                Reaction.LIKE -> updates["likes"] = FieldValue.increment(-1)
                Reaction.DISLIKE -> updates["dislikes"] = FieldValue.increment(-1)
                null -> Unit
            }
            when (reaction) {
                Reaction.LIKE -> updates["likes"] = FieldValue.increment(1)
                Reaction.DISLIKE -> updates["dislikes"] = FieldValue.increment(1)
            }

            firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .update(updates)
                .await()

            reactionRef.set(mapOf("type" to reaction.name)).await()
        }

    override suspend fun removeReaction(reviewId: String): Result<Unit> =
        runCatchingCancellable {
            val uid = currentUserId ?: return@runCatchingCancellable

            val reactionRef = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_REACTIONS)
                .document(uid)

            val existing = reactionRef.get().await()
            val previous = existing.getString("type")?.let {
                runCatching { Reaction.valueOf(it) }.getOrNull()
            } ?: return@runCatchingCancellable

            val field = when (previous) {
                Reaction.LIKE -> "likes"
                Reaction.DISLIKE -> "dislikes"
            }
            firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .update(field, FieldValue.increment(-1))
                .await()
            reactionRef.delete().await()
        }

    // ── Комментарии ──

    override suspend fun loadComments(reviewId: String): Result<List<ReviewComment>> =
        runCatchingCancellable {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .collection(COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(ItemConstraints.COMMENTS_PAGE_SIZE)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toCommentOrNull() }
        }

    override suspend fun loadMoreComments(
        reviewId: String,
        lastCreatedAtMillis: Long
    ): Result<List<ReviewComment>> = runCatchingCancellable {
        val snapshot = firestore.collection(COLLECTION_REVIEWS)
            .document(reviewId)
            .collection(COLLECTION_COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .startAfter(Timestamp(lastCreatedAtMillis / 1000, 0))
            .limit(ItemConstraints.COMMENTS_PAGE_SIZE)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toCommentOrNull() }
    }

    override fun observeComments(reviewId: String): Flow<List<ReviewComment>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_REVIEWS)
            .document(reviewId)
            .collection(COLLECTION_COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toCommentOrNull() }
                    ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: ReviewComment): Result<Unit> =
        runCatchingCancellable {
            val data = mapOf(
                "reviewId" to comment.reviewId,
                "authorId" to comment.authorId,       // ← обязательно
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

    private suspend fun recalculateAverageRating(itemId: String) {
        val snapshot = firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("itemId", itemId)
            .get()
            .await()
        val ratings = snapshot.documents.mapNotNull { it.getLong("rating")?.toInt() }
        val avg = if (ratings.isEmpty()) 0.0 else ratings.average()
        firestore.collection(COLLECTION_ITEMS)
            .document(itemId)
            .update("averageRating", avg)
            .await()
    }

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
        itemIconKey = getString("itemIconKey")
    )

    private fun DocumentSnapshot.toCommentOrNull(): ReviewComment? = runCatching {
        ReviewComment(
            id = id,
            reviewId = getString("reviewId") ?: "",
            authorId = getString("authorId"),   // ← null для анонимных, значение для авторизованных
            authorName = getString("authorName") ?: "",
            authorAvatarBase64 = getString("authorAvatarBase64") ?: "",
            text = getString("text") ?: "",
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
                ?: System.currentTimeMillis()
        )
    }.getOrNull()

    private companion object {
        const val COLLECTION_REVIEWS = "reviews"
        const val COLLECTION_ITEMS = "items"
        const val COLLECTION_COMMENTS = "comments"
        const val COLLECTION_REACTIONS = "reactions"
    }
}