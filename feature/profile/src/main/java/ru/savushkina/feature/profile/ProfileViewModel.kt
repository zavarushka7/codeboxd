package ru.savushkina.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FavouriteRepository
import ru.savushkina.core.domain.FollowRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.AddFavouriteResult
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

private data class FollowCounts(
    val followers: Int = 0,
    val following: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val favouriteRepository: FavouriteRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    /**
     * UID профиля, который открыт: null — свой профиль, non-null — чужой.
     */
    val targetUserId: String? = Routes
        .decode(savedStateHandle.get<String>("userId"))
        .takeIf { it.isNotBlank() }

    val currentUserId: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                is SessionState.Authenticated -> state.user.uid
                is SessionState.Onboarding -> state.userId
                is SessionState.ProfileLoadError -> state.userId
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isOwnProfile: StateFlow<Boolean> = currentUserId
        .map { targetUserId == null || targetUserId == it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), targetUserId == null)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    private val _favourites = MutableStateFlow<List<Item>>(emptyList())
    val favourites = _favourites.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _followersCount = MutableStateFlow(0)
    val followersCount = _followersCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount = _followingCount.asStateFlow()

    val isFollowing: StateFlow<Boolean> = combine(
        currentUserId,
        _refreshTrigger
    ) { uid, _ -> uid }
        .flatMapLatest { uid ->
            val target = targetUserId
            if (uid == null || target == null || uid == target) {
                flowOf(false)
            } else {
                followRepository.observeIsFollowing(uid, target)
                    .catch { emit(false) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _followActionLoading = MutableStateFlow(false)
    val followActionLoading = _followActionLoading.asStateFlow()

    init {
        observeProfile()
        observeReviewsAndFavourites()
        observeFollowCounts()
    }

    /**
     * Pull-to-refresh.
     *
     * ВАЖНО:
     *  - `_refreshTrigger.value++` ДО `try`, чтобы даже при ошибке перечитать
     *    отзывы/избранное/подписки.
     *  - Для своего профиля явно вызываем `userRepository.refresh(uid)` —
     *    иначе `currentUser` может не эмитить новое значение (данные не
     *    изменились / кэш), и UI не обновится.
     *  - Для чужого профиля перечитываем `getUser(targetUserId)`.
     *  - `_isRefreshing` всегда сбрасывается в `finally` — индикатор не зависнет.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            // Форсим пересчёт отзывов/избранного/подписок.
            _refreshTrigger.value += 1
            try {
                val target = targetUserId
                if (target == null) {
                    // Свой профиль: обновляем из Firestore.
                    val uid = currentUserId.value
                    if (uid != null) {
                        userRepository.refresh(uid)
                            .onSuccess { user ->
                                _uiState.value = ProfileUiState.Success(user)
                            }
                            .onFailure { e ->
                                _error.value = e.message ?: "Не удалось обновить профиль"
                            }
                    }
                } else {
                    // Чужой профиль: перечитываем документ.
                    val user = runCatching { userRepository.getUser(target) }
                        .getOrNull()
                    if (user != null) {
                        _uiState.value = ProfileUiState.Success(user)
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            if (targetUserId == null) {
                userRepository.currentUser.collect { user ->
                    _uiState.value = if (user != null) {
                        ProfileUiState.Success(user)
                    } else {
                        ProfileUiState.Error("Профиль не найден")
                    }
                }
            } else {
                val current = currentUserId.filterNotNull().first()
                val isOwn = targetUserId == current
                val user = runCatchingCancellable {
                    if (isOwn) userRepository.refresh(targetUserId).getOrNull()
                    else userRepository.getUser(targetUserId)
                }.getOrNull()

                _uiState.value = if (user != null) ProfileUiState.Success(user)
                else ProfileUiState.Error("Профиль не найден")
            }
        }
    }

    private fun observeReviewsAndFavourites() {
        viewModelScope.launch {
            _refreshTrigger
                .flatMapLatest { currentUserId }
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(emptyList<Review>())
                    } else {
                        val profileUid = targetUserId ?: uid
                        reviewRepository.observeReviewsForUser(profileUid)
                            .catch { _reviews.value = emptyList() }
                    }
                }
                .collect { reviews ->
                    _reviews.value = enrichReviewsWithProfiles(reviews)
                }
        }

        viewModelScope.launch {
            _refreshTrigger
                .flatMapLatest { currentUserId }
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(emptyList<Item>())
                    } else {
                        val profileUid = targetUserId ?: uid
                        favouriteRepository.observeFavourites(profileUid)
                            .map { ids -> itemRepository.getItemsByIds(ids) }
                            .catch { _favourites.value = emptyList() }
                    }
                }
                .collect { _favourites.value = it }
        }
    }

    private fun observeFollowCounts() {
        viewModelScope.launch {
            _refreshTrigger
                .flatMapLatest { currentUserId }
                .flatMapLatest<String?, FollowCounts> { uid ->
                    val profileUid = targetUserId ?: uid
                    when {
                        profileUid == null -> flowOf(FollowCounts())
                        targetUserId == null -> {
                            combine(
                                followRepository.observeFollowers(profileUid)
                                    .map { it.size }
                                    .catch { emit(0) },
                                followRepository.observeFollowing(profileUid)
                                    .map { it.size }
                                    .catch { emit(0) }
                            ) { followers, following ->
                                FollowCounts(followers, following)
                            }
                        }
                        else -> {
                            val result: Result<Pair<Int, Int>> = runCatching {
                                followRepository.getFollowCounts(profileUid)
                            }.getOrElse { Result.failure(it) }

                            val pair: Pair<Int, Int>? = result.getOrNull()
                            val counts = if (pair != null) {
                                FollowCounts(pair.first, pair.second)
                            } else {
                                FollowCounts()
                            }
                            flowOf(counts)
                        }
                    }
                }
                .collect { counts ->
                    _followersCount.value = counts.followers
                    _followingCount.value = counts.following
                }
        }
    }

    fun toggleFollow() {
        val target = targetUserId ?: return
        val uid = currentUserId.value ?: run {
            _error.value = "Не авторизован"
            return
        }
        if (uid == target) return
        if (_followActionLoading.value) return

        viewModelScope.launch {
            _followActionLoading.value = true
            try {
                if (isFollowing.value) {
                    followRepository.unfollow(uid, target).onFailure {
                        _error.value = it.message ?: "Не удалось отписаться"
                    }
                } else {
                    followRepository.follow(uid, target).onFailure {
                        _error.value = it.message ?: "Не удалось подписаться"
                    }
                }
                followRepository.getFollowCounts(target).onSuccess { counts ->
                    _followersCount.value = counts.first
                    _followingCount.value = counts.second
                }
            } finally {
                _followActionLoading.value = false
            }
        }
    }

    private suspend fun enrichReviewsWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val authorIds = reviews.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (authorIds.isEmpty()) return reviews

        val users = runCatching { userRepository.getUsersByIds(authorIds) }
            .getOrDefault(emptyList())
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

    fun addFavourite(itemId: String) {
        val uid = currentUserId.value ?: run {
            _error.value = "Не авторизован"
            return
        }
        viewModelScope.launch {
            favouriteRepository.addFavourite(uid, itemId)
                .onSuccess { result ->
                    _error.value = when (result) {
                        AddFavouriteResult.Added -> null
                        AddFavouriteResult.AlreadyExists -> "Уже в избранном"
                        AddFavouriteResult.LimitReached -> "Максимум 4 избранных"
                    }
                }
                .onFailure { _error.value = it.message ?: "Не удалось добавить" }
        }
    }

    fun removeFavourite(itemId: String) {
        val uid = currentUserId.value ?: run {
            _error.value = "Не авторизован"
            return
        }
        viewModelScope.launch {
            favouriteRepository.removeFavourite(uid, itemId)
                .onFailure { _error.value = it.message ?: "Не удалось убрать" }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signOut() = sessionRepository.signOut()
}
