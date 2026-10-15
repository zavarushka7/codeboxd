package ru.savushkina.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Review
import javax.inject.Inject

data class ReviewFormState(
    val item: Item? = null,
    val rating: Int = 0,
    val comment: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])
    private val currentUserId: String? = authRepository.currentUserId

    private val _state = MutableStateFlow(ReviewFormState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId)
            val existing = currentUserId?.let { reviewRepository.getReview(it, itemId) }

            _state.value = _state.value.copy(
                item = item,
                rating = existing?.rating ?: 0,
                comment = existing?.comment ?: "",
                isLoading = false
            )
        }
    }

    fun onRatingChange(newRating: Int) {
        _state.value = _state.value.copy(rating = newRating, error = null)
    }

    fun onCommentChange(value: String) {
        _state.value = _state.value.copy(comment = value, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            _state.value = _state.value.copy(error = "Не авторизован")
            return
        }

        val rating = _state.value.rating
        if (rating < ItemConstraints.RATING_MIN || rating > ItemConstraints.RATING_MAX) {
            _state.value = _state.value.copy(error = "Поставьте оценку от 0 до 5")
            return
        }

        val comment = _state.value.comment.trim()
        if (comment.length > ItemConstraints.COMMENT_MAX) {
            _state.value = _state.value.copy(
                error = "Комментарий слишком длинный (максимум ${ItemConstraints.COMMENT_MAX} символов)"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val user = userRepository.currentUser.value
            val item = _state.value.item

            reviewRepository.saveReview(
                Review(
                    userId = uid,
                    itemId = itemId,
                    rating = rating,
                    comment = comment,
                    authorName = user?.displayName ?: "",
                    authorAvatarBase64 = user?.avatarBase64 ?: "",
                    itemName = item?.name ?: "",
                    itemSymbol = item?.symbol,
                    itemIconKey = item?.iconKey
                )
            )
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false)
                    onSuccess()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = it.message ?: "Не удалось сохранить отзыв"
                    )
                }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            reviewRepository.deleteReview(uid, itemId)
                .onSuccess { onSuccess() }
                .onFailure {
                    _state.value = _state.value.copy(
                        error = it.message ?: "Не удалось удалить отзыв"
                    )
                }
        }
    }
}