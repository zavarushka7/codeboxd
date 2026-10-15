package ru.savushkina.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.FavouriteRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.User
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val favouriteRepository: FavouriteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val targetUserId: String? = savedStateHandle.get<String>("userId")
    val currentUserId: String? = authRepository.currentUserId

    val isOwnProfile: Boolean = targetUserId == null || targetUserId == currentUserId

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    private val _favourites = MutableStateFlow<List<Item>>(emptyList())
    val favourites = _favourites.asStateFlow()

    init {
        val uid = targetUserId ?: currentUserId

        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Не авторизован")
        } else {
            viewModelScope.launch {
                val user = if (isOwnProfile) {
                    userRepository.refresh(uid).getOrNull()
                } else {
                    userRepository.getUser(uid)
                }
                _uiState.value = if (user != null) ProfileUiState.Success(user)
                else ProfileUiState.Error("Профиль не найден")

                reviewRepository.observeReviewsForUser(uid).collect {
                    _reviews.value = it
                }
            }

            viewModelScope.launch {
                favouriteRepository.observeFavourites(uid).collect { ids ->
                    val items = ids.mapNotNull { itemId ->
                        itemRepository.getItemById(itemId)
                    }
                    _favourites.value = items
                }
            }
        }
    }

    fun addFavourite(itemId: String) {
        val uid = targetUserId ?: currentUserId ?: return
        viewModelScope.launch {
            favouriteRepository.addFavourite(uid, itemId)
        }
    }

    fun removeFavourite(itemId: String) {
        val uid = targetUserId ?: currentUserId ?: return
        viewModelScope.launch {
            favouriteRepository.removeFavourite(uid, itemId)
        }
    }

    fun signOut() {
        sessionRepository.signOut()
    }
}