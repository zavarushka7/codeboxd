package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun WireFrame(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    cornerBrackets: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val baseColor: Color = if (accent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val cornerColor: Color = if (accent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val frameStroke = Sizes.frameStroke.toPx()
            val accentStroke = Sizes.accentStroke.toPx()
            val c = Sizes.cornerLength.toPx()
            val stroke = Stroke(width = frameStroke)

            onDrawBehind {
                drawRect(
                    color = baseColor,
                    topLeft = Offset.Zero,
                    size = size,
                    style = stroke
                )
                if (cornerBrackets) {
                    drawLine(cornerColor, Offset(0f, 0f), Offset(c, 0f), accentStroke)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, c), accentStroke)
                    drawLine(cornerColor, Offset(w - c, 0f), Offset(w, 0f), accentStroke)
                    drawLine(cornerColor, Offset(w, 0f), Offset(w, c), accentStroke)
                    drawLine(cornerColor, Offset(0f, h - c), Offset(0f, h), accentStroke)
                    drawLine(cornerColor, Offset(0f, h), Offset(c, h), accentStroke)
                    drawLine(cornerColor, Offset(w - c, h), Offset(w, h), accentStroke)
                    drawLine(cornerColor, Offset(w, h - c), Offset(w, h), accentStroke)
                }
            }
        },
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WireFramePreview() {
    CodeboxTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WireFrame(modifier = Modifier.size(200.dp, 100.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LabelText(text = "panel")
                }
            }
        }
    }
}