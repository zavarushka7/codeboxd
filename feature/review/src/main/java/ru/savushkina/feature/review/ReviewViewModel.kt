package ru.savushkina.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.ItemConstraints
import ru.savushkina.core.model.Review
import ru.savushkina.core.navigation.Routes
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
    private val savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val itemId: String = Routes.decode(savedStateHandle.get<String>("itemId"))
    private var currentUserId: String? = null

    private val _state = MutableStateFlow(
        ReviewFormState(
            isLoading = true,
            rating = savedStateHandle.get<Int>(KEY_RATING) ?: 0,
            comment = savedStateHandle.get<String>(KEY_COMMENT).orEmpty()
        )
    )
    val state = _state.asStateFlow()

    init {
        if (itemId.isBlank()) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Некорректный ID айтема"
            )
        } else {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            currentUserId = authRepository.authState.firstOrNull()?.uid
            runCatchingCancellable {
                val item = itemRepository.getItemById(itemId)
                val existing = currentUserId?.let { reviewRepository.getReview(it, itemId) }
                item to existing
            }.onSuccess { (item, existing) ->
                // Если в SavedStateHandle уже есть черновик (rating/comment),
                // не перезатираем его данными из существующего отзыва.
                val hasDraftRating = savedStateHandle.contains(KEY_RATING)
                val hasDraftComment = savedStateHandle.contains(KEY_COMMENT)
                _state.value = _state.value.copy(
                    item = item,
                    rating = if (hasDraftRating) _state.value.rating
                    else existing?.rating ?: 0,
                    comment = if (hasDraftComment) _state.value.comment
                    else existing?.comment ?: "",
                    isLoading = false
                )
                // Если черновика не было, а отзыв существовал — сохраняем как стартовый.
                if (!hasDraftRating && existing != null) {
                    savedStateHandle[KEY_RATING] = existing.rating
                }
                if (!hasDraftComment && existing != null) {
                    savedStateHandle[KEY_COMMENT] = existing.comment
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Не удалось загрузить айтем"
                )
            }
        }
    }

    fun onRatingChange(newRating: Int) {
        savedStateHandle[KEY_RATING] = newRating
        _state.value = _state.value.copy(rating = newRating, error = null)
    }

    fun onCommentChange(value: String) {
        savedStateHandle[KEY_COMMENT] = value
        _state.value = _state.value.copy(comment = value, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            _state.value = _state.value.copy(error = "Не авторизован")
            return
        }
        val rating = _state.value.rating
        if (rating < 1 || rating > ItemConstraints.RATING_MAX) {
            _state.value = _state.value.copy(error = "Поставьте оценку от 1 до 5")
            return
        }
        val comment = _state.value.comment.trim()
        if (comment.length > ItemConstraints.REVIEW_COMMENT_MAX) {
            _state.value = _state.value.copy(
                error = "Комментарий слишком длинный (максимум ${ItemConstraints.REVIEW_COMMENT_MAX})"
            )
            return
        }
        if (_state.value.isSaving) return

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
                    // Черновик больше не нужен — очищаем.
                    savedStateHandle.remove<Int>(KEY_RATING)
                    savedStateHandle.remove<String>(KEY_COMMENT)
                    _state.value = _state.value.copy(
                        isSaving = false,
                        rating = 0,
                        comment = ""
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = e.message ?: "Не удалось сохранить отзыв"
                    )
                }
        }
    }

    private companion object {
        const val KEY_RATING = "review_rating"
        const val KEY_COMMENT = "review_comment"
    }
}