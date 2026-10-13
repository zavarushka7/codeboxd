package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun WireTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val textColor = if (enabled) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier.fillMaxWidth()) {
        WireFrame(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.inputHeight),
            accent = enabled && value.isNotEmpty()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = Spacing.md),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        LabelText(text = placeholder)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                        cursorBrush = SolidColor(accent),
                        visualTransformation = visualTransformation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier
                            .width(Sizes.iconFrame)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
        }

        if (label != null) {
            Box(
                modifier = Modifier
                    .padding(start = Spacing.md, top = Spacing.xs),
                contentAlignment = Alignment.TopStart
            ) {
                LabelText(text = label, color = accent)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WireTextFieldPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WireTextField(
                value = "",
                onValueChange = {},
                label = "email",
                placeholder = "dev@codebox.app"
            )
            WireTextField(
                value = "alena@codebox.app",
                onValueChange = {},
                label = "email"
            )
        }
    }
}