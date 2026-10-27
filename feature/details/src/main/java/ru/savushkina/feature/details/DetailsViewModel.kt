package ru.savushkina.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.flow.update
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

    private val _isLoadingMoreReviews = MutableStateFlow(false)
    val isLoadingMoreReviews = _isLoadingMoreReviews.asStateFlow()

    private val _canLoadMoreReviews = MutableStateFlow(false)
    val canLoadMoreReviews = _canLoadMoreReviews.asStateFlow()

    private val _loadMoreReviewsError = MutableStateFlow<String?>(null)
    val loadMoreReviewsError = _loadMoreReviewsError.asStateFlow()

    private var nextReviewCursor: String? = null

    private val _retry = MutableStateFlow(0)

    private val currentUid = authRepository.authState
        .map { it?.uid }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUid())

    /**
     * Флаг «мы уже видели не-null Item». Переживает повторные null и
     * позволяет отличить «документа нет» от «ещё не загрузился».
     */
    @Volatile
    private var hasSeenItem: Boolean = false

    init {
        if (itemId.isBlank()) {
            _uiState.value = DetailsUiState.Error("Некорректный ID айтема")
        } else {
            observe()
        }
    }

    private fun observe() {
        viewModelScope.launch {
            // Job, который через NULL_TIMEOUT_MS переведёт Loading → Error,
            // если Item так и не пришёл. Отменяется при получении Item
            // или при новом retry.
            var pendingNullJob: Job? = null

            _retry
                .flatMapLatest {
                    _uiState.value = DetailsUiState.Loading
                    hasSeenItem = false
                    nextReviewCursor = null
                    _canLoadMoreReviews.value = false
                    _loadMoreReviewsError.value = null
                    pendingNullJob?.cancel()
                    pendingNullJob = null

                    combine(
                        itemRepository.observeItemById(itemId),
                        currentUid.flatMapLatest { uid ->
                            reviewRepository.observeReviewsForItem(
                                itemId = itemId,
                                limit = ReviewRepository.DEFAULT_REVIEWS_PAGE_SIZE
                            )
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

                    when {
                        item != null -> {
                            // Пришёл реальный Item — отменяем дедлайн,
                            // запоминаем, что документ существует.
                            hasSeenItem = true
                            pendingNullJob?.cancel()
                            pendingNullJob = null
                            applySuccess(item = item, uid = quad.uid, reviews = quad.reviews)
                        }
                        hasSeenItem -> {
                            // Item уже был, а теперь null — значит его удалили.
                            pendingNullJob?.cancel()
                            pendingNullJob = null
                            _uiState.value = DetailsUiState.Error("Айтем не найден")
                        }
                        else -> {
                            // Первый эмит null — НЕ ставим Error сразу.
                            // Firestore может прислать «пустой» кэш-снапшот
                            // до реального. Запускаем таймаут: если за
                            // NULL_TIMEOUT_MS Item не придёт, значит документа нет.
                            if (pendingNullJob == null) {
                                pendingNullJob = viewModelScope.launch {
                                    delay(NULL_TIMEOUT_MS)
                                    if (!hasSeenItem && _uiState.value is DetailsUiState.Loading) {
                                        _uiState.value = DetailsUiState.Error("Айтем не найден")
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun applySuccess(item: Item, uid: String?, reviews: List<Review>) {
        viewModelScope.launch {
            val freshReviews = enrichWithProfiles(reviews)
            val myReview = uid?.let { u -> freshReviews.find { it.userId == u } }

            val headIds = freshReviews.map { it.reviewId }.toSet()
            val currentExtra = when (val s = _uiState.value) {
                is DetailsUiState.Success ->
                    s.reviews.filterNot { it.reviewId in headIds }
                else -> emptyList()
            }
            val merged = (freshReviews + currentExtra).distinctBy { it.reviewId }

            if (nextReviewCursor == null) {
                nextReviewCursor = if (
                    freshReviews.size >= ReviewRepository.DEFAULT_REVIEWS_PAGE_SIZE
                ) {
                    freshReviews.lastOrNull()?.reviewId
                } else null
                _canLoadMoreReviews.value = nextReviewCursor != null
            }

            _uiState.value = DetailsUiState.Success(item, merged, myReview)
        }
    }

    fun loadMoreReviews() {
        if (_isLoadingMoreReviews.value) return
        if (!_canLoadMoreReviews.value) return
        val cursor = nextReviewCursor ?: return
        if (_loadMoreReviewsError.value != null) return

        _isLoadingMoreReviews.value = true
        _loadMoreReviewsError.value = null

        viewModelScope.launch {
            reviewRepository.loadMoreReviews(
                itemId = itemId,
                lastDocId = cursor,
                limit = ReviewRepository.DEFAULT_REVIEWS_PAGE_SIZE
            ).onSuccess { page ->
                val fresh = enrichWithProfiles(page.items)
                _uiState.update { state ->
                    if (state is DetailsUiState.Success) {
                        val merged = (state.reviews + fresh).distinctBy { it.reviewId }
                        state.copy(reviews = merged)
                    } else state
                }
                nextReviewCursor = page.nextCursor
                _canLoadMoreReviews.value = page.nextCursor != null
            }.onFailure { e ->
                _loadMoreReviewsError.value = e.message ?: "Не удалось загрузить ещё"
            }
            _isLoadingMoreReviews.value = false
        }
    }

    fun retryLoadMoreReviews() {
        if (_loadMoreReviewsError.value == null) return
        _loadMoreReviewsError.value = null
        loadMoreReviews()
    }

    private suspend fun enrichWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val authorIds = reviews.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (authorIds.isEmpty()) return reviews

        val users: List<User> = runCatching {
            userRepository.getUsersByIds(authorIds)
        }.getOrDefault(emptyList())
        if (users.isEmpty()) return reviews

        val byId = users.associateBy { it.uid }
        return reviews.map { review ->
            val user = byId[review.userId]
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

    private companion object {
        /**
         * Сколько ждать первого не-null Item после первого эмита null.
         * 1500 мс — с запасом на холодный старт и медленную сеть.
         * Если дольше — считаем, что документа нет.
         */
        const val NULL_TIMEOUT_MS = 1500L
    }
}