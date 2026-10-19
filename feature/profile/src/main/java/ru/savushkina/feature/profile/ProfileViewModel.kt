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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FavouriteRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {

    private val targetUserId: String? = Routes.decode(savedStateHandle.get<String>("userId"))
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

    init {
        observeProfile()
        observeReviewsAndFavourites()
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
            currentUserId
                .flatMapLatest { uid ->
                    if (uid == null) {
                        kotlinx.coroutines.flow.flowOf(emptyList<Review>())
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
            currentUserId
                .flatMapLatest { uid ->
                    if (uid == null) {
                        kotlinx.coroutines.flow.flowOf(emptyList<Item>())
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

    /**
     * Подменяет денормализованные authorName / authorAvatarBase64
     * актуальными значениями из users/{authorId}.
     *
     * Один проход по уникальным authorId: каждый автор запрашивается
     * ровно один раз за вызов. Для 5–20 отзывов — 1–5 запросов.
     */
    private suspend fun enrichReviewsWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val authorIds = reviews.map { it.userId }.distinct()
        val profiles = mutableMapOf<String, User>()
        for (uid in authorIds) {
            if (uid.isBlank()) continue
            val user = runCatching { userRepository.getUser(uid) }.getOrNull() ?: continue
            profiles[uid] = user
        }
        return reviews.map { review ->
            val user = profiles[review.userId]
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