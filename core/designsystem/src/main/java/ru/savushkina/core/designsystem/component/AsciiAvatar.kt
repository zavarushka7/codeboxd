package ru.savushkina.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.savushkina.core.designsystem.theme.JetBrainsMono
import kotlin.math.min
import kotlin.math.pow

private const val ASCII_SOURCE_PX = 256

private const val ASCII_COLS = 50
private const val CHAR_WIDTH_FACTOR = 2.0f

private const val CHAR_SIZE_FACTOR = 0.9f

private const val CONTRAST_CLIP_LOW = 0.05f
private const val CONTRAST_CLIP_HIGH = 0.95f
private const val GAMMA = 1.3f
private const val SIGMOID_STRENGTH = 0.5f

private val ASCII_PALETTE = "@#W8$&0Qoab+-:. ".toCharArray()
private val asciiCache = LruCache<String, String>(64)

@Composable
fun AsciiAvatar(
    avatarBase64: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    fillWidth: Boolean = false,
    showFrame: Boolean = true,
    clickable: Boolean = true
) {
    var showFullscreen by remember { mutableStateOf(false) }

    val ascii: String? = remember(avatarBase64) {
        if (avatarBase64.isBlank()) null
        else asciiCache.get(avatarBase64)
            ?: decodeBase64ForAscii(avatarBase64)
                ?.toAscii(cols = ASCII_COLS)
                ?.also { asciiCache.put(avatarBase64, it) }
    }

    val frameModifier = if (fillWidth) {
        modifier.fillMaxWidth().aspectRatio(1f)
    } else {
        modifier.size(size)
    }.then(
        if (clickable && ascii != null) Modifier.clickable { showFullscreen = true }
        else Modifier
    )

    val content: @Composable BoxScope.() -> Unit = {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            val sideDp = min(maxWidth.value, maxHeight.value)
            if (ascii != null) {
                val charSize = (sideDp / ASCII_COLS * CHAR_SIZE_FACTOR).sp
                Box(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    AsciiText(
                        text = ascii,
                        charSize = charSize,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = CHAR_WIDTH_FACTOR
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            } else {
                AsciiAvatarFallback(fallbackText, sideDp.dp)
            }
        }
    }

    if (showFrame) {
        WireFrame(modifier = frameModifier, accent = true, content = content)
    } else {
        Box(modifier = frameModifier, content = content)
    }

    if (showFullscreen && ascii != null) {
        AsciiAvatarFullscreen(ascii = ascii, onDismiss = { showFullscreen = false })
    }
}

@Composable
private fun AsciiText(
    text: String,
    charSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = charSize,
            lineHeight = charSize,
            letterSpacing = 0.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
}

@Composable
private fun AsciiAvatarFullscreen(
    ascii: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val side = min(maxWidth.value, maxHeight.value)
                // ВАЖНО: то же число колонок, что и при генерации строки.
                val charSize = (side / ASCII_COLS * CHAR_SIZE_FACTOR).sp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    AsciiText(
                        text = ascii,
                        charSize = charSize,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = CHAR_WIDTH_FACTOR
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AsciiAvatarFallback(text: String, avatarSize: Dp) {
    val fontSize = when {
        avatarSize.value < 36f -> 12
        avatarSize.value < 56f -> 16
        avatarSize.value < 80f -> 22
        avatarSize.value < 100f -> 28
        else -> 36
    }
    Text(
        text = text.take(2).uppercase(),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMono,
        color = MaterialTheme.colorScheme.primary
    )
}

// ── Декодирование ──

private fun decodeBase64ForAscii(base64: String): Bitmap? = runCatching {
    val bytes = Base64.decode(base64, Base64.DEFAULT)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, ASCII_SOURCE_PX)
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

// ── Bitmap → ASCII ──

private fun Bitmap.toAscii(cols: Int = ASCII_COLS): String {
    require(cols > 0) { "cols must be > 0" }

    val side = minOf(width, height)
    val left = (width - side) / 2
    val top = (height - side) / 2
    val square = Bitmap.createBitmap(this, left, top, side, side)
    val scaled = Bitmap.createScaledBitmap(square, cols, cols, true)

    val lum = FloatArray(cols * cols)
    for (y in 0 until cols) {
        for (x in 0 until cols) {
            val p = scaled.getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[y * cols + x] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }
    }

    val sorted = lum.copyOf()
    sorted.sort()
    val lo = sorted[(sorted.size * CONTRAST_CLIP_LOW).toInt().coerceIn(0, sorted.lastIndex)]
    val hi = sorted[(sorted.size * CONTRAST_CLIP_HIGH).toInt().coerceIn(0, sorted.lastIndex)]
    val range = (hi - lo).coerceAtLeast(1e-3f)

    val sb = StringBuilder(cols * (cols + 1))
    val lastIndex = ASCII_PALETTE.size - 1

    for (y in 0 until cols) {
        for (x in 0 until cols) {
            val normalized = ((lum[y * cols + x] - lo) / range).coerceIn(0f, 1f)
            val gammaCorrected = normalized.pow(GAMMA)
            val contrasted = applySigmoid(gammaCorrected, SIGMOID_STRENGTH)
            val inverted = 1f - contrasted
            val idx = (inverted * lastIndex).toInt().coerceIn(0, lastIndex)
            sb.append(ASCII_PALETTE[idx])
        }
        if (y < cols - 1) sb.append('\n')
    }

    if (square !== this) square.recycle()
    if (scaled !== square) scaled.recycle()

    return sb.toString()
}

private fun applySigmoid(x: Float, strength: Float): Float {
    if (strength <= 0f) return x
    val c = x.coerceIn(0f, 1f)
    val s = c * c * (3f - 2f * c)
    return (c + (s - c) * strength).coerceIn(0f, 1f)
}