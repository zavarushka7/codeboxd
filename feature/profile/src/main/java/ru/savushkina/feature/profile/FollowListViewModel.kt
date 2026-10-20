package ru.savushkina.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FollowRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.FollowListMode
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface FollowListUiState {
    data object Loading : FollowListUiState
    data class Success(val users: List<User>) : FollowListUiState
    data class Error(val message: String) : FollowListUiState
}

@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    val mode: FollowListMode = FollowListMode.fromRoute(
        Routes.decode(savedStateHandle.get<String>("mode"))
    )

    /** Целевой uid: если передан — чужой профиль, иначе — свой. */
    private val targetUserId: String? = Routes.decode(savedStateHandle.get<String>("userId"))
        .takeIf { it.isNotBlank() }

    private val currentUserId: String? = run {
        val state = sessionRepository.sessionState.value
        when (state) {
            is SessionState.Authenticated -> state.user.uid
            is SessionState.Onboarding -> state.userId
            is SessionState.ProfileLoadError -> state.userId
            else -> null
        }
    }
    fun FollowListMode.routeTitle(isOwn: Boolean): String = when (this) {
        FollowListMode.FOLLOWERS -> "// подписчики"
        FollowListMode.FOLLOWING -> "// подписки"
    }

    /** true — смотрим свой список. */
    val isOwnList: Boolean
        get() = targetUserId == null || targetUserId == currentUserId

    private val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = FollowListUiState.Loading
            runCatchingCancellable {
                val uid = targetUserId
                    ?: currentUserId
                    ?: throw IllegalStateException("Не авторизован")

                val ids: List<String> = when (mode) {
                    FollowListMode.FOLLOWERS ->
                        followRepository.observeFollowers(uid).firstOrNull().orEmpty()
                    FollowListMode.FOLLOWING ->
                        followRepository.observeFollowing(uid).firstOrNull().orEmpty()
                }
                if (ids.isEmpty()) return@runCatchingCancellable emptyList()

                userRepository.getUsersByIds(ids)
            }.onSuccess { users ->
                _uiState.value = FollowListUiState.Success(users)
            }.onFailure { e ->
                _uiState.value = FollowListUiState.Error(
                    e.message ?: "Не удалось загрузить список"
                )
            }
        }
    }
}