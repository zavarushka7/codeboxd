package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun WireFrame(
    modifier: Modifier = Modifier,
    accent: Boolean = false, // флаг активного состояния
    cornerBrackets: Boolean = true, // можно ли отключить уголки
    /**
     * слот API
     * @Composable - лямбда сама является composable (может содержать Text, Button и тд)
     * BoxScope.() -> Unit - лямбда с receiver'ом типа BoxScope. то есть внутри content доступны методы BoxScope: alogn(), matchParentSize и тд
     * = {} - пустая лямба по умолчанию (компонент можно вызывать без контента)
     * Зачем BoxScope?
     * Чтобы вызывающий мог выравнивать содержиое
     */
    content: @Composable BoxScope.() -> Unit = {}
) {
    val baseColor = if (accent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val cornerColor = if (accent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    Box(
        /**
         * drawBehind - модификатор, который позволяет рисовать позади содержимого
         */
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height

            /** Базовая рамка — тонкая
             * drawReact - рисует прямоугольник
             * Offset.Zero - левый верхний угол
             * size - весь размер области
             * style = Stroke(width= ...) - не заливка, а обводка заданной толщины
             */
            drawRect(
                color = baseColor,
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(width = Sizes.frameStroke.toPx())
            )

            // Уголки-скобки — толще
            if (cornerBrackets) {
                val c = Sizes.cornerLength.toPx()
                val s = Sizes.accentStroke.toPx()

                drawLine(cornerColor, Offset(0f, 0f), Offset(c, 0f), s)
                drawLine(cornerColor, Offset(0f, 0f), Offset(0f, c), s)
                drawLine(cornerColor, Offset(w - c, 0f), Offset(w, 0f), s)
                drawLine(cornerColor, Offset(w, 0f), Offset(w, c), s)
                drawLine(cornerColor, Offset(0f, h - c), Offset(0f, h), s)
                drawLine(cornerColor, Offset(0f, h), Offset(c, h), s)
                drawLine(cornerColor, Offset(w - c, h), Offset(w, h), s)
                drawLine(cornerColor, Offset(w, h - c), Offset(w, h), s)
            }
        },
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WireFramePreview() {
    CodeboxTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WireFrame(modifier = Modifier.size(200.dp, 100.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LabelText(text = "panel")
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WireFrameAccentPreview() {
    CodeboxTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WireFrame(
                modifier = Modifier.size(200.dp, 100.dp),
                accent = true
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LabelText(text = "active")
                }
            }
        }
    }
}