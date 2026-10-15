package ru.savushkina.core.designsystem.util

import androidx.compose.runtime.staticCompositionLocalOf
import ru.savushkina.core.model.TextCaseStyle

/**
 * Текущий стиль текста. Провайдится через CompositionLocalProvider в CodeboxTheme.
 * Компоненты читают его сами — не надо пробрасывать через параметры.
 */
val LocalTextCaseStyle = staticCompositionLocalOf { TextCaseStyle.NORMAL }

/**
 * Применяет стиль к строке.
 *
 * Поддерживает префиксы:
 * - "// " — стиль применяется только к части после префикса.
 * - "[ ... ]" — стиль применяется только к содержимому в скобках.
 */
fun String.toDisplayCase(style: TextCaseStyle): String {
    if (style == TextCaseStyle.NORMAL) return this

    // Префикс "// "
    if (startsWith("// ")) {
        val body = removePrefix("// ")
        return "// " + body.transform(style)
    }

    // Квадратные скобки [ ... ]
    val bracket = Regex("^(\\[\\s*)(.*?)(\\s*])$").find(this)
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
        .mapIndexed { i, w ->
            if (i == 0) w else w.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")

    TextCaseStyle.SNAKE_CASE -> lowercase()
        .replace(" ", "_")
        .replace("-", "_")

    TextCaseStyle.NORMAL -> this
}