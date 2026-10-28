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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface ReviewListUiState {
    data object Loading : ReviewListUiState
    data class Success(val reviews: List<Review>) : ReviewListUiState
    data class Error(val message: String) : ReviewListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReviewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /** userId из route; null — свой профиль. */
    private val targetUserId: String? = Routes
        .decode(savedStateHandle.get<String>("userId"))
        .takeIf { it.isNotBlank() }

    /**
     * Эмитит только не-null uid. Пока сессия ещё не восстановилась — не эмитит
     * ничего, поэтому мы не показываем «Не авторизован» и не мигаем
     * Loading → Error → Loading.
     */
    private val currentUserId: StateFlow<String?> = sessionRepository.sessionState
        .map { state ->
            when (state) {
                is SessionState.Authenticated -> state.user.uid
                is SessionState.Onboarding -> state.userId
                is SessionState.ProfileLoadError -> state.userId
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<ReviewListUiState>(ReviewListUiState.Loading)
    val uiState: StateFlow<ReviewListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserId
                // Пока uid не пришёл — ждём, никаких Error.
                .filterNotNull()
                .flatMapLatest { current ->
                    val uid = targetUserId ?: current
                    reviewRepository.observeReviewsForUser(uid)
                        .map { reviews ->
                            ReviewListUiState.Success(enrichWithProfiles(reviews))
                                    as ReviewListUiState
                        }
                        .catch { e ->
                            emit(
                                ReviewListUiState.Error(
                                    e.message ?: "Не удалось загрузить отзывы"
                                )
                            )
                        }
                }
                .collect { _uiState.value = it }
        }
    }

    private suspend fun enrichWithProfiles(reviews: List<Review>): List<Review> {
        if (reviews.isEmpty()) return reviews
        val ids = reviews.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return reviews
        val users = runCatching { userRepository.getUsersByIds(ids) }
            .getOrDefault(emptyList())
        if (users.isEmpty()) return reviews
        val byId = users.associateBy(User::uid)
        return reviews.map { review ->
            val user = byId[review.userId] ?: return@map review
            review.copy(
                authorName = user.displayName,
                authorAvatarBase64 = user.avatarBase64
            )
        }
    }
}