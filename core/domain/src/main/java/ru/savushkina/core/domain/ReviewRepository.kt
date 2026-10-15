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

    fun observeMyReaction(reviewId: String): Flow<Reaction?>
    suspend fun react(reviewId: String, reaction: Reaction): Result<Unit>
    suspend fun removeReaction(reviewId: String): Result<Unit>

    suspend fun loadComments(reviewId: String): Result<List<ReviewComment>>
    suspend fun loadMoreComments(reviewId: String, lastCreatedAtMillis: Long): Result<List<ReviewComment>>
    fun observeComments(reviewId: String): Flow<List<ReviewComment>>
    suspend fun addComment(comment: ReviewComment): Result<Unit>
    suspend fun deleteComment(reviewId: String, commentId: String): Result<Unit>
}