package ru.savushkina.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.savushkina.core.domain.AvatarEncodeResult
import ru.savushkina.core.domain.AvatarEncoder
import ru.savushkina.core.model.UserConstraints
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarEncoderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AvatarEncoder {

    override suspend fun encode(uri: Uri): AvatarEncodeResult =
        withContext(Dispatchers.IO) { encodeInternal(uri) }

    private fun encodeInternal(uri: Uri): AvatarEncodeResult {
        val bytes: ByteArray = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return AvatarEncodeResult.Failure("Не удалось прочитать изображение")
        } catch (e: Exception) {
            Log.e(TAG, "encode: read failed", e)
            return AvatarEncodeResult.Failure("Не удалось прочитать изображение")
        }

        if (bytes.isEmpty()) return AvatarEncodeResult.Failure("Файл пустой")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return AvatarEncodeResult.Failure("Формат изображения не поддерживается")
        }

        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_PX)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: return AvatarEncodeResult.Failure("Не удалось декодировать изображение")

        val scaled: Bitmap = try {
            if (bitmap.width != TARGET_PX || bitmap.height != TARGET_PX) {
                Bitmap.createScaledBitmap(bitmap, TARGET_PX, TARGET_PX, true)
                    .also { if (it !== bitmap) bitmap.recycle() }
            } else {
                bitmap
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "encode: OOM", e)
            bitmap.recycle()
            return AvatarEncodeResult.Failure("Недостаточно памяти для обработки изображения")
        }

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        scaled.recycle()

        val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        return if (base64.length > UserConstraints.AVATAR_MAX_BASE64_LENGTH) {
            AvatarEncodeResult.TooLarge(base64.length)
        } else {
            AvatarEncodeResult.Success(base64)
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

    private companion object {
        const val TAG = "AvatarEncoderImpl"
        const val TARGET_PX = 128
        const val JPEG_QUALITY = 70
    }
}