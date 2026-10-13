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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun SegmentedBar(
    progress: Float,
    segments: Int = 40,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val filled = (segments * progress.coerceIn(0f, 1f)).toInt()

    Canvas(modifier = modifier.fillMaxWidth().height(Sizes.segmentedBarHeight)) {
        val gap = 2f
        val segW = (size.width - gap * (segments - 1)) / segments
        repeat(segments) { i ->
            drawRect(
                color = if (i < filled) accent else outline,
                topLeft = Offset(i * (segW + gap), 0f),
                size = Size(segW, size.height)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SegmentedBarPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SegmentedBar(progress = 1f)
            SegmentedBar(progress = 0.7f)
            SegmentedBar(progress = 0.3f)
        }
    }
}