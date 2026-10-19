package ru.savushkina.core.designsystem.util

import androidx.compose.runtime.staticCompositionLocalOf
import ru.savushkina.core.model.TextCaseStyle

val LocalTextCaseStyle = staticCompositionLocalOf { TextCaseStyle.NORMAL }

private val BRACKET_REGEX = Regex("^(\\[\\s*)(.*?)(\\s*])$")
private const val COMMENT_PREFIX = "// "

fun String.toDisplayCase(style: TextCaseStyle): String {
    if (style == TextCaseStyle.NORMAL) return this

    if (startsWith(COMMENT_PREFIX)) {
        val body = removePrefix(COMMENT_PREFIX)
        return COMMENT_PREFIX + body.transform(style)
    }

    val bracket = BRACKET_REGEX.find(this)
    if (bracket != null) {
        val (open, content, close) = bracket.destructured
        return open + content.transform(style) + close
    }

    return transform(style)
}

private fun String.transform(style: TextCaseStyle): String = when (style) {
    TextCaseStyle.CAMEL_CASE -> lowercase()
        .split(" ", "_", "-")
        .filter { it.isNotBlank() }
        .mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar { it.uppercase() } }
        .joinToString("")

    TextCaseStyle.SNAKE_CASE -> lowercase()
        .replace(" ", "_")
        .replace("-", "_")

    TextCaseStyle.NORMAL -> this
}