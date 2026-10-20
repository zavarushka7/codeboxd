package ru.savushkina.core.domain

import android.net.Uri

sealed interface AvatarEncodeResult {
    data class Success(val base64: String) : AvatarEncodeResult
    data class TooLarge(val base64Length: Int) : AvatarEncodeResult
    data class Failure(val reason: String) : AvatarEncodeResult
}

interface AvatarEncoder {
    /**
     * Кодирует изображение из [uri] в base64 (JPEG, scaled to 128x128).
     * Возвращает [AvatarEncodeResult] — success, too_large или failure.
     */
    suspend fun encode(uri: Uri): AvatarEncodeResult
}