package ru.savushkina.core.domain

import android.graphics.Bitmap
import android.net.Uri

sealed interface AvatarEncodeResult {
    data class Success(val base64: String) : AvatarEncodeResult
    data class TooLarge(val base64Length: Int) : AvatarEncodeResult
    data class Failure(val reason: String) : AvatarEncodeResult
}

interface AvatarEncoder {
    suspend fun encode(uri: Uri): AvatarEncodeResult

    /** Новый путь: готовый квадратный Bitmap из ImageCropView. */
    suspend fun encodeBitmap(bitmap: Bitmap): AvatarEncodeResult

    @Deprecated("Use encodeBitmap(bitmap) with ImageCropView")
    suspend fun encodeCropped(
        uri: Uri,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): AvatarEncodeResult = encode(uri)
}