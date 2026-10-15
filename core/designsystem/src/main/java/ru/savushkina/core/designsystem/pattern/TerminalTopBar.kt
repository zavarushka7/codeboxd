package ru.savushkina.core.designsystem.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun TerminalTopBar(
    title: String,
    modifier: Modifier = Modifier,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) { left?.invoke() }
        LabelText(text = title)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            right?.invoke()
        }
    }
}