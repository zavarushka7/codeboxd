package ru.savushkina.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.image.cropview.CropType
import com.image.cropview.ImageCrop
import com.image.cropview.ImageCropView
import com.image.cropview.rememberSaveableImageCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun AvatarCropper(
    uri: Uri,
    onDismiss: () -> Unit,
    onResult: (bitmap: Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
                    var sample = 1
                    while (maxSide / sample > 2048) sample *= 2
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                }
            }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.lg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LabelText(text = "// выберите квадрат аватара")
                Spacer(Modifier.height(Spacing.md))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (bmp == null) {
                        BlinkingCursor()
                    } else {
                        // state holder живёт в этом блоке, но кнопка «готово»
                        // находится снаружи. Решение: хранить imageCrop в
                        // remember на уровне AvatarCropper через var.
                        // Проще — использовать CompositionLocal или
                        // вынести кнопку внутрь блока с bitmap.
                        // Ниже — кнопка вынесена ВНУТРЬ этого Box через
                        // отдельный Column, чтобы иметь доступ к imageCrop.
                        CropContent(
                            bitmap = bmp,
                            onDismiss = onDismiss,
                            onResult = onResult
                        )
                    }
                }
            }
        }
    }
}

/**
 * Внутренний composable, который создаёт imageCrop и содержит кнопку «готово».
 * Так imageCrop и кнопка находятся в одной области видимости.
 */
@Composable
private fun CropContent(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onResult: (Bitmap) -> Unit
) {
    val imageCrop: ImageCrop = rememberSaveableImageCrop(bitmap)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ImageCropView(
                imageCrop = imageCrop,
                modifier = Modifier.fillMaxSize(),
                cropType = CropType.SQUARE,
                guideLineColor = Color.LightGray,
                guideLineWidth = 2.dp,
                showGuideLines = true
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WireButton(text = "отмена", onClick = onDismiss)
            }
            Box(modifier = Modifier.weight(1f)) {
                WireButton(
                    text = "готово",
                    onClick = { onResult(imageCrop.onCrop()) },
                    accent = true
                )
            }
        }
    }
}