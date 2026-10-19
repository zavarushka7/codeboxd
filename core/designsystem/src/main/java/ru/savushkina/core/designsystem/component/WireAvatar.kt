package ru.savushkina.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val AVATAR_TARGET_PX = 256

/**
 * Кэш декодированных аватаров.
 *
 * ВАЖНО: Bitmap в кэше НЕ вызывают recycle().
 *
 * Причина: ImageBitmap, полученный через Bitmap.asImageBitmap(), НЕ копирует
 * пиксели — он оборачивает тот же android.graphics.Bitmap. Если вызвать
 * bitmap.recycle(), ImageBitmap станет невалидным, и при следующей отрисовке
 * Canvas.drawBitmap() бросит "trying to use a recycled bitmap".
 *
 * Освобождение нативной памяти при вытеснении из LruCache происходит
 * через GC: Bitmap становится недостижим, GC финализирует его, а
 * NativeAllocationRegistry (API 26+) освобождает нативные пиксели.
 * Для аватаров 256x256 (~256 КБ) при лимите кэша ~1/8 maxMemory это
 * безопасно и не приводит к OOM.
 *
 * Если когда-нибудь понадобится детерминированное освобождение — перейти
 * на Coil (AsyncImage), который сам управляет жизненным циклом Bitmap.
 */
private val avatarCache: LruCache<String, Bitmap> by lazy {
    val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        // entryRemoved НЕ переопределяем: recycle() здесь приведёт к крашу,
        // если ImageBitmap, оборачивающий этот Bitmap, ещё используется в UI.
    }
}

@Composable
fun WireAvatar(
    avatarBase64: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val bitmap: Bitmap? = remember(avatarBase64) {
        if (avatarBase64.isBlank()) {
            null
        } else {
            avatarCache.get(avatarBase64) ?: decodeBase64(avatarBase64)?.also {
                avatarCache.put(avatarBase64, it)
            }
        }
    }

    WireFrame(
        modifier = modifier.size(size),
        accent = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            // isRecycled — защита от случая, если Bitmap всё же оказался
            // recycled (например, сторонним кодом). Тогда показываем fallback.
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AvatarFallback(fallbackText, size)
            }
        }
    }
}

private fun decodeBase64(base64: String): Bitmap? = runCatching {
    val bytes = Base64.decode(base64, Base64.DEFAULT)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, AVATAR_TARGET_PX)

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}.getOrNull()

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

/**
 * Размер шрифта fallback-буквы зависит от размера аватара,
 * иначе для 28.dp аватара буква 28.sp выглядит огромной и вылезает.
 */
private fun fallbackFontSize(avatarSize: Dp): Int = when {
    avatarSize.value < 36f -> 12
    avatarSize.value < 56f -> 16
    avatarSize.value < 80f -> 22
    avatarSize.value < 100f -> 28
    else -> 36
}

@Composable
private fun AvatarFallback(text: String, avatarSize: Dp) {
    Text(
        text = text.take(2).uppercase(),
        fontSize = fallbackFontSize(avatarSize).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}