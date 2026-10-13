package ru.savushkina.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CodeboxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CodeboxColorScheme,
        typography = CodeboxTypography,
        shapes = CodeboxShapes,
        content = content
    )
}