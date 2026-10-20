package com.example.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.User
import javax.inject.Inject

sealed interface UserSearchUiState {
    data object Idle : UserSearchUiState
    data object Loading : UserSearchUiState
    data class Success(val users: List<User>) : UserSearchUiState
    data class Error(val message: String) : UserSearchUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val uiState: StateFlow<UserSearchUiState> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { raw ->
            val q = raw.trim().removePrefix("@")
            flow {
                if (q.isBlank()) {
                    emit(UserSearchUiState.Idle)
                } else {
                    emit(UserSearchUiState.Loading)
                    runCatchingCancellable {
                        val byPrefix = userRepository.searchUsersByUsernamePrefix(q, limit = 30)
                        if (byPrefix.isNotEmpty()) return@runCatchingCancellable byPrefix
                        // Точное совпадение, если префикс не дал результата.
                        val exact = userRepository.getUserByUsername(q)
                        if (exact != null) listOf(exact) else emptyList()
                    }
                        .onSuccess { emit(UserSearchUiState.Success(it)) }
                        .onFailure {
                            emit(UserSearchUiState.Error(it.message ?: "Не удалось найти"))
                        }
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserSearchUiState.Idle
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }
}