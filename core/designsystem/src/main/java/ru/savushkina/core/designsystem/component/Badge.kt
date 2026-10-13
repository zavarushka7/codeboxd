package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    WireFrame(modifier = modifier, accent = accent, cornerBrackets = false) {
        Box(modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)) {
            LabelText(
                text = text,
                color = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BadgePreview() {
    CodeboxTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Badge(text = "live")
            Badge(text = "human", accent = true)
            Badge(text = "offline")
        }
    }
}