package ru.savushkina.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import ru.savushkina.core.designsystem.util.LocalTextCaseStyle
import ru.savushkina.core.designsystem.util.toDisplayCase

@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val style = LocalTextCaseStyle.current

    Text(
        text = text.toDisplayCase(style),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}