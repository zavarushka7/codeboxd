package ru.savushkina.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment
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
    authRepository: AuthRepository
) : ViewModel() {

    private val reviewId: String = checkNotNull(savedStateHandle["reviewId"])
    val currentUserId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<ReviewDetailUiState>(ReviewDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _comments = MutableStateFlow<List<ReviewComment>>(emptyList())
    val comments = _comments.asStateFlow()

    /**
     * Флаг, что первая загрузка комментариев завершена.
     * Пока false — экран не показывается, чтобы не мигало "нет комментариев".
     */
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

    private var commentsJob: Job? = null

    init {
        viewModelScope.launch {
            val parts = reviewId.split("_")
            if (parts.size < 2) {
                _uiState.value = ReviewDetailUiState.Error("Некорректный ID отзыва")
                return@launch
            }
            val userId = parts[0]
            val itemId = parts.drop(1).joinToString("_")
            val review = reviewRepository.getReview(userId, itemId)
            _uiState.value = if (review != null) ReviewDetailUiState.Success(review)
            else ReviewDetailUiState.Error("Отзыв не найден")

            commentsJob = viewModelScope.launch {
                // 1. Первая загрузка (свежая)
                reviewRepository.loadComments(reviewId)
                    .onSuccess { loaded ->
                        _comments.value = loaded
                        _canLoadMoreComments.value =
                            loaded.size >= ItemConstraints.COMMENTS_PAGE_SIZE
                    }

                // 2. Подписка на обновления
                reviewRepository.observeComments(reviewId).collect { all ->
                    _comments.value = all
                    _canLoadMoreComments.value =
                        all.size >= ItemConstraints.COMMENTS_PAGE_SIZE
                    // Помечаем, что первая загрузка завершена
                    if (!_commentsLoaded.value) _commentsLoaded.value = true
                }
            }
        }
    }

    fun loadMoreComments() {
        val last = _comments.value.lastOrNull() ?: return
        if (_isLoadingMoreComments.value) return
        viewModelScope.launch {
            _isLoadingMoreComments.value = true
            reviewRepository.loadMoreComments(reviewId, last.createdAtMillis)
                .onSuccess { more ->
                    _comments.value = _comments.value + more
                    _canLoadMoreComments.value =
                        more.size >= ItemConstraints.COMMENTS_PAGE_SIZE
                }
            _isLoadingMoreComments.value = false
        }
    }

    fun onCommentDraftChange(value: String) {
        _commentDraft.value = value
    }

    fun submitComment() {
        val text = _commentDraft.value.trim()
        if (text.isBlank()) return
        if (text.length > ItemConstraints.REVIEW_COMMENT_MAX) return

        viewModelScope.launch {
            _isSendingComment.value = true
            val currentUser = userRepository.currentUser.value
            reviewRepository.addComment(
                ReviewComment(
                    reviewId = reviewId,
                    authorId = currentUserId,
                    authorName = currentUser?.displayName ?: "",
                    authorAvatarBase64 = currentUser?.avatarBase64 ?: "",
                    text = text
                )
            ).onSuccess { _commentDraft.value = "" }
            _isSendingComment.value = false
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            reviewRepository.deleteComment(reviewId, commentId)
        }
    }
}