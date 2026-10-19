package ru.savushkina.feature.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    @ApplicationContext private val context: Context,
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

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _error.value = null
            val result = withContext(Dispatchers.IO) { encodeImage(uri, context) }
            when (result) {
                is EncodeResult.Failure -> _error.value = result.reason
                is EncodeResult.TooLarge -> _error.value =
                    "Изображение слишком большое (${result.base64Length / 1024} KB > " +
                            "${UserConstraints.AVATAR_MAX_BASE64_LENGTH / 1024} KB)"
                is EncodeResult.Success -> {
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
                description = currentDraft.description.trim(),
                avatarBase64 = currentDraft.avatarBase64,
                role = currentDraft.role
            )
                .onSuccess {
                    _isSaving.value = false
                    // Сбрасываем draft — при следующем входе на экран
                    // он снова заполнится из currentUser.
                    _draft.value = null
                    onSuccess()
                }
                .onFailure {
                    _isSaving.value = false
                    _error.value = it.message ?: "Не удалось сохранить"
                    // ВАЖНО: при ошибке draft НЕ сбрасываем — пользователь
                    // должен видеть свои правки и иметь возможность повторить.
                    // Но при уходе с экрана draft будет сброшен в onCleared().
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _draft.value = null
    }

    private fun validate(user: User): String? {
        val name = user.name.trim()
        if (name.isBlank()) return "Имя не может быть пустым"
        if (name.length > UserConstraints.NAME_MAX) {
            return "Имя слишком длинное (максимум ${UserConstraints.NAME_MAX} символов)"
        }
        if (user.description.length > UserConstraints.DESCRIPTION_MAX) {
            return "Описание слишком длинное (максимум ${UserConstraints.DESCRIPTION_MAX})"
        }
        return null
    }
}

// ────────────────────────────────────────────────────────────────
// Кодирование аватара.
// ────────────────────────────────────────────────────────────────

private sealed interface EncodeResult {
    data class Success(val base64: String) : EncodeResult
    data class TooLarge(val base64Length: Int) : EncodeResult
    data class Failure(val reason: String) : EncodeResult
}

private const val AVATAR_TARGET_PX = 128
private const val JPEG_QUALITY = 70

private fun encodeImage(uri: Uri, context: Context): EncodeResult {
    val bytes: ByteArray = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return EncodeResult.Failure("Не удалось прочитать изображение")
    } catch (e: Exception) {
        Log.e("SettingsViewModel", "encodeImage: read failed", e)
        return EncodeResult.Failure("Не удалось прочитать изображение")
    }

    if (bytes.isEmpty()) return EncodeResult.Failure("Файл пустой")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return EncodeResult.Failure("Формат изображения не поддерживается")
    }

    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, AVATAR_TARGET_PX)
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        ?: return EncodeResult.Failure("Не удалось декодировать изображение")

    val scaled: Bitmap = try {
        if (bitmap.width != AVATAR_TARGET_PX || bitmap.height != AVATAR_TARGET_PX) {
            Bitmap.createScaledBitmap(bitmap, AVATAR_TARGET_PX, AVATAR_TARGET_PX, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        } else {
            bitmap
        }
    } catch (e: OutOfMemoryError) {
        Log.e("SettingsViewModel", "encodeImage: OOM", e)
        bitmap.recycle()
        return EncodeResult.Failure("Недостаточно памяти для обработки изображения")
    }

    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
    scaled.recycle()

    val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

    return if (base64.length > UserConstraints.AVATAR_MAX_BASE64_LENGTH) {
        EncodeResult.TooLarge(base64.length)
    } else {
        EncodeResult.Success(base64)
    }
}

private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= target && h / 2 >= target) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
}