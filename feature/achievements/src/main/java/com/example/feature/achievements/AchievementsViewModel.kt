package ru.savushkina.feature.achievements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AchievementRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Achievement
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.navigation.Routes
import javax.inject.Inject

sealed interface AchievementsUiState {
    data object Loading : AchievementsUiState
    data class Success(val achievements: List<Achievement>) : AchievementsUiState
    data class Error(val message: String) : AchievementsUiState
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val achievementRepository: AchievementRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /** userId из роута (для чужого профиля) или null (свой). */
    private val targetUserId: String? = Routes.decode(savedStateHandle.get<String>("userId"))
        .takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = AchievementsUiState.Loading
            val uid = targetUserId ?: currentUserId()
            if (uid == null) {
                _uiState.value = AchievementsUiState.Error("Не авторизован")
                return@launch
            }
            achievementRepository.computeAchievements(uid)
                .onSuccess { _uiState.value = AchievementsUiState.Success(it) }
                .onFailure {
                    _uiState.value = AchievementsUiState.Error(
                        it.message ?: "Не удалось загрузить награды"
                    )
                }
        }
    }

    private suspend fun currentUserId(): String? {
        val state = sessionRepository.sessionState.firstOrNull()
        return when (state) {
            is SessionState.Authenticated -> state.user.uid
            is SessionState.Onboarding -> state.userId
            is SessionState.ProfileLoadError -> state.userId
            else -> userRepository.currentUser.value?.uid
        }
    }
}