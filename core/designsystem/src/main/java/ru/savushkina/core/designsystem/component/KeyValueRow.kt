package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.savushkina.core.designsystem.util.LocalTextCaseStyle
import ru.savushkina.core.designsystem.util.toDisplayCase
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun KeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val style = LocalTextCaseStyle.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelText(text = key)
        androidx.compose.material3.Text(
            text = value.toDisplayCase(style),
            style = MaterialTheme.typography.titleMedium,
            color = if (accent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}