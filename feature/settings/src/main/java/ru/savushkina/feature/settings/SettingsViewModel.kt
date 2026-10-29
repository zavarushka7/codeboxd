package ru.savushkina.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.AvatarEncodeResult
import ru.savushkina.core.domain.AvatarEncoder
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.TextCaseStyle
import ru.savushkina.core.model.User
import ru.savushkina.core.model.UserConstraints
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val user: User) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val avatarEncoder: AvatarEncoder
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _draft = MutableStateFlow<User?>(null)
    val draft = _draft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername = _isCheckingUsername.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable = _usernameAvailable.asStateFlow()

    private var usernameCheckJob: Job? = null

    val textCaseStyle: StateFlow<TextCaseStyle> = settingsRepository.textCaseStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TextCaseStyle.NORMAL)

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = SettingsUiState.Success(user)
                    if (_draft.value == null) _draft.value = user
                } else if (_uiState.value !is SettingsUiState.Loading) {
                    _uiState.value = SettingsUiState.Error("Не авторизован")
                }
            }
        }
    }

    fun setTextCaseStyle(style: TextCaseStyle) {
        viewModelScope.launch { settingsRepository.setTextCaseStyle(style) }
    }

    fun onNameChange(value: String) {
        _draft.update { it?.copy(name = value) }
        _error.value = null
    }

    fun onUsernameChange(value: String) {
        val clean = value.lowercase().removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }
        _draft.update { it?.copy(username = clean) }
        _error.value = null
        _usernameAvailable.value = null
        scheduleUsernameCheck(clean)
    }

    private fun scheduleUsernameCheck(username: String) {
        usernameCheckJob?.cancel()
        val original = (_uiState.value as? SettingsUiState.Success)?.user?.username.orEmpty()
        if (username == original) {
            _usernameAvailable.value = true
            return
        }
        if (username.isBlank()) return
        if (!UserConstraints.USERNAME_REGEX.matches(username)) return

        usernameCheckJob = viewModelScope.launch {
            delay(500)
            _isCheckingUsername.value = true
            userRepository.isUsernameAvailable(username)
                .onSuccess { available ->
                    _isCheckingUsername.value = false
                    _usernameAvailable.value = available
                    if (!available) _error.value = "Этот username уже занят"
                }
                .onFailure {
                    _isCheckingUsername.value = false
                    _usernameAvailable.value = null
                }
        }
    }

    fun onDescriptionChange(value: String) {
        _draft.update { it?.copy(description = value) }
        _error.value = null
    }

    fun onRoleChange(role: Role) {
        _draft.update { user ->
            if (user == null) null
            else user.copy(role = if (user.role == role) null else role)
        }
        _error.value = null
    }

    fun onAvatarSelected(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _error.value = null
            when (val result = avatarEncoder.encodeBitmap(bitmap)) {
                is AvatarEncodeResult.Failure -> _error.value = result.reason
                is AvatarEncodeResult.TooLarge -> _error.value =
                    "Изображение слишком большое (${result.base64Length / 1024} KB > " +
                            "${UserConstraints.AVATAR_MAX_BASE64_LENGTH / 1024} KB)"
                is AvatarEncodeResult.Success -> {
                    _draft.update { it?.copy(avatarBase64 = result.base64) }
                }
            }
        }
    }
    fun signOut() = sessionRepository.signOut()

    fun save(onSuccess: () -> Unit) {
        val currentDraft = _draft.value ?: return
        validate(currentDraft)?.let { _error.value = it; return }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            val authUser = authRepository.authState.firstOrNull()
            if (authUser == null) {
                _isSaving.value = false
                _error.value = "Сессия истекла, войдите заново"
                return@launch
            }
            userRepository.saveProfile(
                uid = authUser.uid,
                name = currentDraft.name.trim(),
                username = currentDraft.username.trim().ifBlank { null },
                description = currentDraft.description.trim(),
                avatarBase64 = currentDraft.avatarBase64,
                role = currentDraft.role
            )
                .onSuccess {
                    _isSaving.value = false
                    _draft.value = null
                    _usernameAvailable.value = null
                    onSuccess()
                }
                .onFailure {
                    _isSaving.value = false
                    _error.value = it.message ?: "Не удалось сохранить"
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _draft.value = null
        usernameCheckJob?.cancel()
    }

    private fun validate(user: User): String? {
        val name = user.name.trim()
        if (name.isBlank()) return "Имя не может быть пустым"
        if (name.length > UserConstraints.NAME_MAX) {
            return "Имя слишком длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        if (user.username.isNotBlank() &&
            !UserConstraints.USERNAME_REGEX.matches(user.username)
        ) {
            return "Username: 3–20 символов, a-z, 0-9, _"
        }
        if (_usernameAvailable.value == false) return "Этот username уже занят"
        if (user.description.length > UserConstraints.DESCRIPTION_MAX) {
            return "Описание слишком длинное (максимум ${UserConstraints.DESCRIPTION_MAX})"
        }
        return null
    }
}