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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme

@Composable
fun HorizontalLine(
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val color = if (accent) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = if (accent) 3f else 1.5f
        )
    }
}
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HorizontalLinePreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HorizontalLine()
            HorizontalLine(accent = true)
        }
    }
}