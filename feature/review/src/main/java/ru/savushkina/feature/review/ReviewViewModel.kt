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
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val itemId: String = Routes.decode(savedStateHandle.get<String>("itemId"))
    private var currentUserId: String? = null

    private val _state = MutableStateFlow(ReviewFormState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        if (itemId.isBlank()) {
            _state.value = ReviewFormState(error = "Некорректный ID айтема")
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
                _state.value = _state.value.copy(
                    item = item,
                    rating = existing?.rating ?: 0,
                    comment = existing?.comment ?: "",
                    isLoading = false
                )
            }.onFailure { e ->
                // Не блокируем форму — пользователь всё ещё может поставить оценку.
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Не удалось загрузить айтем"
                )
            }
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
                    _state.value = _state.value.copy(isSaving = false)
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
}