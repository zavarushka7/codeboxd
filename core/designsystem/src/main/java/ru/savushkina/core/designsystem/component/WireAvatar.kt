package ru.savushkina.core.designsystem.component

import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun WireAvatar(
    avatarBase64: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp
) {
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
            if (avatarBase64.isNotBlank()) {
                val bitmap = remember(avatarBase64) {
                    runCatching {
                        val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AvatarFallback(fallbackText)
                }
            } else {
                AvatarFallback(fallbackText)
            }
        }
    }
}

@Composable
private fun AvatarFallback(text: String) {
    Text(
        text = text.take(2).uppercase(),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary
    )
}