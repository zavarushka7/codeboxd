package ru.savushkina.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.savushkina.core.designsystem.util.LocalTextCaseStyle
import ru.savushkina.core.model.TextCaseStyle

@Composable
fun CodeboxTheme(
    textCaseStyle: TextCaseStyle = TextCaseStyle.NORMAL,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTextCaseStyle provides textCaseStyle) {
        MaterialTheme(
            colorScheme = CodeboxColorScheme,
            typography = CodeboxTypography,
            shapes = CodeboxShapes,
            content = content
        )
    }
}