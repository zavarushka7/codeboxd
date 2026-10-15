package ru.savushkina.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import javax.inject.Inject

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(
        val item: Item,
        val reviews: List<Review>,
        val myReview: Review?
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])
    val currentUserId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<String, Reaction?>>(emptyMap())
    val myReactions = _myReactions.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                itemRepository.observeItemById(itemId),       // ← Flow<Item?>
                reviewRepository.observeReviewsForItem(itemId),
                flowOfMyReview()
            ) { item, reviews, myReview ->
                if (item == null) {
                    DetailsUiState.Error("Айтем не найден")
                } else {
                    reviews.forEach { observeMyReaction(it.reviewId) }
                    DetailsUiState.Success(item, reviews, myReview)
                }
            }.collect { _uiState.value = it }
        }
    }

    private fun flowOfMyReview() = flow {
        val uid = currentUserId
        emit(if (uid != null) reviewRepository.getReview(uid, itemId) else null)
    }

    private fun observeMyReaction(reviewId: String) {
        if (_myReactions.value.containsKey(reviewId)) return
        viewModelScope.launch {
            reviewRepository.observeMyReaction(reviewId).collect { reaction ->
                _myReactions.value = _myReactions.value + (reviewId to reaction)
            }
        }
    }

    fun onLike(reviewId: String) {
        val current = _myReactions.value[reviewId]
        viewModelScope.launch {
            if (current == Reaction.LIKE) reviewRepository.removeReaction(reviewId)
            else reviewRepository.react(reviewId, Reaction.LIKE)
        }
    }

    fun onDislike(reviewId: String) {
        val current = _myReactions.value[reviewId]
        viewModelScope.launch {
            if (current == Reaction.DISLIKE) reviewRepository.removeReaction(reviewId)
            else reviewRepository.react(reviewId, Reaction.DISLIKE)
        }
    }
}