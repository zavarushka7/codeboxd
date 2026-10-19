package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment

interface ReviewRepository {
    fun observeReviewsForUser(userId: String): Flow<List<Review>>
    fun observeReviewsForItem(itemId: String): Flow<List<Review>>

    suspend fun getReview(userId: String, itemId: String): Review?
    suspend fun saveReview(review: Review): Result<Unit>
    suspend fun deleteReview(userId: String, itemId: String): Result<Unit>

    fun observeMyReaction(reviewId: String, userId: String): Flow<Reaction?>
    suspend fun react(reviewId: String, userId: String, reaction: Reaction): Result<Unit>
    suspend fun removeReaction(reviewId: String, userId: String): Result<Unit>

    suspend fun loadComments(reviewId: String): Result<List<ReviewComment>>
    suspend fun loadMoreComments(
        reviewId: String,
        lastCreatedAtMillis: Long,
        lastId: String
    ): Result<List<ReviewComment>>
    fun observeComments(reviewId: String): Flow<List<ReviewComment>>
    suspend fun addComment(comment: ReviewComment): Result<Unit>
    suspend fun deleteComment(reviewId: String, commentId: String): Result<Unit>

    /**
     * Реакции текущего пользователя на набор отзывов.
     * Один listener на всю коллекцию `reactions`, фильтр — `userId == uid`
     * + `reviewId in chunk(30)` (Firestore ограничивает whereIn 30 значениями).
     */
    fun observeMyReactionsForReviews(
        reviewIds: List<String>,
        userId: String
    ): Flow<Map<String, Reaction?>>
}