package ru.savushkina.feature.settings

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.domain.UserRepository
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.TextCaseStyle
import ru.savushkina.core.model.User
import ru.savushkina.core.model.UserConstraints
import java.io.ByteArrayOutputStream
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _draft = MutableStateFlow<User?>(null)
    val draft = _draft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val textCaseStyle: StateFlow<TextCaseStyle> = settingsRepository.textCaseStyle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TextCaseStyle.NORMAL
        )

    fun setTextCaseStyle(style: TextCaseStyle) {
        viewModelScope.launch {
            settingsRepository.setTextCaseStyle(style)
        }
    }
    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = SettingsUiState.Success(user)
                    if (_draft.value == null) {
                        _draft.value = user
                    }
                }
            }
        }

        viewModelScope.launch {
            val uid = authRepository.currentUserId
            if (uid != null) {
                userRepository.refresh(uid)
            }
        }
    }

    fun onNameChange(value: String) {
        _draft.value = _draft.value?.copy(name = value)
        _error.value = null
    }

    fun onDescriptionChange(value: String) {
        _draft.value = _draft.value?.copy(description = value)
        _error.value = null
    }

    fun onRoleChange(role: Role) {
        // Если нажали на уже выбранную роль — снимаем выбор (role = null)
        val current = _draft.value?.role
        val newRole = if (current == role) null else role
        _draft.value = _draft.value?.copy(role = newRole)
        _error.value = null
    }

    fun onAvatarSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            val base64 = encodeImage(uri, context) ?: run {
                _error.value = "не_удалось_загрузить_изображение"
                return@launch
            }
            if (base64.length > UserConstraints.AVATAR_MAX_BYTES) {
                _error.value = "изображение_слишком_большое"
                return@launch
            }
            _draft.value = _draft.value?.copy(avatarBase64 = base64)
            _error.value = null
        }
    }
    fun signOut() {
        sessionRepository.signOut()
    }
    fun save(onSuccess: () -> Unit) {
        val draft = _draft.value ?: return
        val uid = authRepository.currentUserId
        if (uid == null) {
            _error.value = "сессия_истекла, войдите_заново"
            return
        }

        validate(draft)?.let {
            _error.value = it
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            userRepository.saveProfile(
                uid = uid,
                name = draft.name.trim(),
                description = draft.description.trim(),
                avatarBase64 = draft.avatarBase64,
                role = draft.role
            )
                .onSuccess {
                    _isSaving.value = false
                    _draft.value = null
                    onSuccess()
                }
                .onFailure {
                    _isSaving.value = false
                    _error.value = it.message ?: "не_удалось_сохранить"
                }
        }
    }

    private fun validate(user: User): String? {
        val name = user.name.trim()
        if (name.isBlank()) return "имя_не_может_быть_пустым"
        if (name.length > UserConstraints.NAME_MAX) {
            return "имя_слишком_длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        if (user.description.length > UserConstraints.DESCRIPTION_MAX) {
            return "описание_слишком_длинное (максимум ${UserConstraints.DESCRIPTION_MAX} символов)"
        }
        return null
    }
}

private suspend fun encodeImage(uri: Uri, context: Context): String? =
    runCatching {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()