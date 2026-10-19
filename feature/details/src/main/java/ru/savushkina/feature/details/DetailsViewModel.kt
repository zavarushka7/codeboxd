package ru.savushkina.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.Routes
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val itemId: String = Routes.decode(savedStateHandle.get<String>("itemId"))

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<String, Reaction?>>(emptyMap())
    val myReactions = _myReactions.asStateFlow()

    private val _retry = MutableStateFlow(0)

    // Кэш профилей авторов: чтобы не дёргать users/{uid} на каждое
    // обновление списка отзывов.
    private val profileCache = mutableMapOf<String, User>()

    private val currentUid = authRepository.authState
        .map { it?.uid }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUid())

    init {
        if (itemId.isBlank()) {
            _uiState.value = DetailsUiState.Error("Некорректный ID айтема")
        } else {
            observe()
        }
    }

    private fun observe() {
        viewModelScope.launch {
            _retry
                .flatMapLatest {
                    _uiState.value = DetailsUiState.Loading
                    combine(
                        itemRepository.observeItemById(itemId),
                        currentUid.flatMapLatest { uid ->
                            reviewRepository.observeReviewsForItem(itemId)
                                .flatMapLatest { reviews ->
                                    if (uid == null || reviews.isEmpty()) {
                                        flowOf(reviews to emptyMap<String, Reaction?>())
                                    } else {
                                        combine(
                                            flowOf(reviews),
                                            reviewRepository.observeMyReactionsForReviews(
                                                reviewIds = reviews.map { it.reviewId },
                                                userId = uid
                                            )
                                        ) { r, reactions -> r to reactions }
                                    }
                                }
                                .map { (reviews, reactions) -> Triple(uid, reviews, reactions) }
                        }
                    ) { item, (uid, reviews, reactions) ->
                        Quad(item, uid, reviews, reactions)
                    }.catch { e ->
                        _uiState.value = DetailsUiState.Error(
                            e.message ?: "Не удалось загрузить айтем"
                        )
                        emit(Quad(null, null, emptyList(), emptyMap()))
                    }
                }
                .collect { quad ->
                    _myReactions.value = quad.reactions
                    val item = quad.item
                    if (item == null) {
                        _uiState.value = DetailsUiState.Error("Айтем не найден")
                    } else {
                        // Подтягиваем актуальные имена/аватары авторов.
                        val freshReviews = enrichWithProfiles(quad.reviews)
                        val myReview = quad.uid?.let { u ->
                            freshReviews.find { it.userId == u }
                        }
                        _uiState.value = DetailsUiState.Success(item, freshReviews, myReview)
                    }
                }
        }
    }

    /**
     * Подменяет денормализованные authorName / authorAvatarBase64
     * актуальными значениями из users/{authorId}.
     * Кэширует профили в пределах ViewModel.
     */
    private suspend fun enrichWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val authorIds = reviews.map { it.userId }.distinct()
        for (uid in authorIds) {
            if (uid.isBlank()) continue
            if (profileCache.containsKey(uid)) continue
            val user = runCatching { userRepository.getUser(uid) }.getOrNull()
            if (user != null) {
                profileCache[uid] = user
            }
        }
        return reviews.map { review ->
            val user = profileCache[review.userId]
            if (user == null) review
            else review.copy(
                authorName = user.displayName,
                authorAvatarBase64 = user.avatarBase64
            )
        }
    }

    fun retry() {
        _retry.value++
    }

    fun onLike(reviewId: String) {
        val uid = authRepository.currentUid() ?: return
        viewModelScope.launch {
            val current = _myReactions.value[reviewId]
            if (current == Reaction.LIKE) {
                reviewRepository.removeReaction(reviewId, uid)
            } else {
                reviewRepository.react(reviewId, uid, Reaction.LIKE)
            }
        }
    }

    fun onDislike(reviewId: String) {
        val uid = authRepository.currentUid() ?: return
        viewModelScope.launch {
            val current = _myReactions.value[reviewId]
            if (current == Reaction.DISLIKE) {
                reviewRepository.removeReaction(reviewId, uid)
            } else {
                reviewRepository.react(reviewId, uid, Reaction.DISLIKE)
            }
        }
    }

    private data class Quad(
        val item: Item?,
        val uid: String?,
        val reviews: List<Review>,
        val reactions: Map<String, Reaction?>
    )
}