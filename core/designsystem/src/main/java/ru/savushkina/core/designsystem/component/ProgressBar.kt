package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Boolean = true
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val fill = if (accent) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier.fillMaxWidth().height(Sizes.progressBarHeight)) {
        val stroke = 2f
        drawRect(
            color = outline,
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = stroke)
        )
        drawRect(
            color = fill,
            topLeft = Offset(stroke, stroke),
            size = Size(
                (size.width - stroke * 2) * progress.coerceIn(0f, 1f),
                size.height - stroke * 2
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProgressBarPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressBar(progress = 0.25f)
            ProgressBar(progress = 0.5f)
            ProgressBar(progress = 0.85f)
            ProgressBar(progress = 1f)
        }
    }
}