package ru.savushkina.core.designsystem.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    topPadding: Dp = Spacing.lg,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = topPadding,
                bottom = Spacing.lg
            )
    ) {
        content()
    }
}