package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalLine(
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val color = if (accent) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    val strokeDp = if (accent) 3.dp else 1.5.dp

    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeDp.toPx()
        )
    }
}