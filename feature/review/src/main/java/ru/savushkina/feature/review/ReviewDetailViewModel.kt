package ru.savushkina.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface ReviewDetailUiState {
    data object Loading : ReviewDetailUiState
    data class Success(val review: Review) : ReviewDetailUiState
    data class Error(val message: String) : ReviewDetailUiState
}

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val reviewId: String = Routes.decode(savedStateHandle.get<String>("reviewId"))

    /** Если пришли из Activity по комментарию — его id. */
    val initialCommentId: String? = Routes.decode(savedStateHandle.get<String>("commentId"))
        .takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<ReviewDetailUiState>(ReviewDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _comments = MutableStateFlow<List<ReviewComment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _commentsLoaded = MutableStateFlow(false)
    val commentsLoaded = _commentsLoaded.asStateFlow()

    private val _commentDraft = MutableStateFlow("")
    val commentDraft = _commentDraft.asStateFlow()

    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment = _isSendingComment.asStateFlow()

    private val _canLoadMoreComments = MutableStateFlow(false)
    val canLoadMoreComments = _canLoadMoreComments.asStateFlow()

    private val _isLoadingMoreComments = MutableStateFlow(false)
    val isLoadingMoreComments = _isLoadingMoreComments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var hasLoadedMoreThanPage = false

    init {
        val parsed = Review.parseReviewId(reviewId)
        if (parsed == null) {
            _uiState.value = ReviewDetailUiState.Error("Некорректный ID отзыва")
        } else {
            val (userId, itemId) = parsed
            load(userId, itemId)
            observeComments()
        }
    }

    private fun load(userId: String, itemId: String) {
        viewModelScope.launch {
            val review = reviewRepository.getReview(userId, itemId)
            _uiState.value = if (review != null) {
                val fresh = enrichReviewsWithProfiles(listOf(review)).first()
                ReviewDetailUiState.Success(fresh)
            } else {
                ReviewDetailUiState.Error("Отзыв не найден")
            }
        }
    }

    private fun observeComments() {
        viewModelScope.launch {
            reviewRepository.observeComments(reviewId)
                .catch { e ->
                    _error.value = e.message ?: "Не удалось загрузить комментарии"
                    _commentsLoaded.value = true
                }
                .collect { tail ->
                    val hasMore = tail.size > ItemConstraints.COMMENTS_PAGE_SIZE
                    val visible = if (hasMore)
                        tail.takeLast(ItemConstraints.COMMENTS_PAGE_SIZE.toInt())
                    else tail
                    val visibleIds = visible.map { it.id }.toSet()
                    val head = _comments.value.filterNot { it.id in visibleIds }

                    val merged = (head + visible)
                        .distinctBy { it.id }
                        .sortedWith(
                            compareBy<ReviewComment> { it.createdAtMillis }
                                .thenBy { it.id }
                        )

                    _comments.value = enrichCommentsWithProfiles(merged)
                    _commentsLoaded.value = true

                    if (!hasLoadedMoreThanPage) {
                        _canLoadMoreComments.value = hasMore
                    }

                    // Если пришли по commentId, а его ещё нет — догружаем страницы,
                    // пока не найдём (или пока не кончатся).
                    val target = initialCommentId
                    if (target != null &&
                        _comments.value.none { it.id == target } &&
                        _canLoadMoreComments.value &&
                        !_isLoadingMoreComments.value
                    ) {
                        loadMoreComments()
                    }
                }
        }
    }

    fun loadMoreComments() {
        val oldest = _comments.value.firstOrNull() ?: return
        if (_isLoadingMoreComments.value) return
        viewModelScope.launch {
            _isLoadingMoreComments.value = true
            runCatchingCancellable {
                reviewRepository.loadMoreComments(
                    reviewId = reviewId,
                    lastCreatedAtMillis = oldest.createdAtMillis,
                    lastId = oldest.id
                ).getOrThrow()
            }.onSuccess { more ->
                val existingIds = _comments.value.map { it.id }.toSet()
                val newOnes = more.filterNot { it.id in existingIds }
                val merged = (_comments.value + newOnes)
                    .distinctBy { it.id }
                    .sortedWith(
                        compareBy<ReviewComment> { it.createdAtMillis }
                            .thenBy { it.id }
                    )
                _comments.value = enrichCommentsWithProfiles(merged)
                _canLoadMoreComments.value =
                    more.size >= ItemConstraints.COMMENTS_PAGE_SIZE
                if (more.isNotEmpty()) {
                    hasLoadedMoreThanPage = true
                }
            }.onFailure {
                _error.value = it.message ?: "Не удалось загрузить комментарии"
            }
            _isLoadingMoreComments.value = false
        }
    }

    fun onCommentDraftChange(value: String) {
        _commentDraft.value = value
    }

    fun submitComment(onSuccess: () -> Unit = {}) {
        val text = _commentDraft.value.trim()
        if (text.isBlank()) { _error.value = "Комментарий пустой"; return }
        if (text.length > ItemConstraints.COMMENT_MAX) {
            _error.value = "Комментарий слишком длинный"; return
        }

        viewModelScope.launch {
            _isSendingComment.value = true
            val uid = authRepository.authState.firstOrNull()?.uid
            if (uid == null) {
                _error.value = "Не авторизован"
                _isSendingComment.value = false
                return@launch
            }
            val currentUser = userRepository.currentUser.value
            reviewRepository.addComment(
                ReviewComment(
                    reviewId = reviewId,
                    authorId = uid,
                    authorName = currentUser?.displayName ?: "",
                    authorAvatarBase64 = currentUser?.avatarBase64 ?: "",
                    text = text
                )
            )
                .onSuccess {
                    _commentDraft.value = ""
                    _error.value = null
                    onSuccess()
                }
                .onFailure {
                    _error.value = it.message ?: "Не удалось отправить комментарий"
                }
            _isSendingComment.value = false
        }
    }

    fun deleteComment(commentId: String) {
        val removed = _comments.value.find { it.id == commentId }
        _comments.value = _comments.value.filterNot { it.id == commentId }
        viewModelScope.launch {
            reviewRepository.deleteComment(reviewId, commentId)
                .onFailure {
                    if (removed != null) {
                        _comments.value = (_comments.value + removed)
                            .distinctBy { it.id }
                            .sortedWith(
                                compareBy<ReviewComment> { it.createdAtMillis }
                                    .thenBy { it.id }
                            )
                    }
                    _error.value = it.message ?: "Не удалось удалить"
                }
        }
    }

    private suspend fun enrichReviewsWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val ids = reviews.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return reviews
        val users = runCatching { userRepository.getUsersByIds(ids) }
            .getOrDefault(emptyList())
        if (users.isEmpty()) return reviews
        val byId = users.associateBy { it.uid }
        return reviews.map { review ->
            val user: User = byId[review.userId] ?: return@map review
            review.copy(
                authorName = user.displayName,
                authorAvatarBase64 = user.avatarBase64
            )
        }
    }

    private suspend fun enrichCommentsWithProfiles(
        comments: List<ReviewComment>
    ): List<ReviewComment> {
        if (comments.isEmpty()) return comments
        val ids = comments.mapNotNull { it.authorId }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return comments
        val users = runCatching { userRepository.getUsersByIds(ids) }
            .getOrDefault(emptyList())
        if (users.isEmpty()) return comments
        val byId = users.associateBy { it.uid }
        return comments.map { comment ->
            val uid = comment.authorId ?: return@map comment
            val user = byId[uid] ?: return@map comment
            comment.copy(
                authorName = user.displayName,
                authorAvatarBase64 = user.avatarBase64
            )
        }
    }
}