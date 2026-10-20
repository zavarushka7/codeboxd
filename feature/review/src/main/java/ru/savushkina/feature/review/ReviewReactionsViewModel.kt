package ru.savushkina.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

data class ReactionUser(
    val user: User,
    val reaction: Reaction,
    val createdAtMillis: Long
)

sealed interface ReactionsUiState {
    data object Loading : ReactionsUiState
    data class Success(val items: List<ReactionUser>) : ReactionsUiState
    data class Error(val message: String) : ReactionsUiState
}

@HiltViewModel
class ReviewReactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val reviewId: String = Routes.decode(savedStateHandle.get<String>("reviewId"))

    private val _uiState = MutableStateFlow<ReactionsUiState>(ReactionsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ReactionsUiState.Loading
            runCatchingCancellable {
                val reactions: List<ReviewRepository.ReactionEntry> =
                    reviewRepository.observeReactionsForReview(reviewId).first()

                val uids = reactions.map { it.userId }.distinct()
                val users = userRepository.getUsersByIds(uids).associateBy { it.uid }
                reactions.mapNotNull { r ->
                    val user = users[r.userId] ?: return@mapNotNull null
                    ReactionUser(
                        user = user,
                        reaction = r.reaction,
                        createdAtMillis = r.createdAtMillis
                    )
                }.sortedByDescending { it.createdAtMillis }
            }.onSuccess { _uiState.value = ReactionsUiState.Success(it) }
                .onFailure { _uiState.value = ReactionsUiState.Error(it.message ?: "Ошибка") }
        }
    }
}