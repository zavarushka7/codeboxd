package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun WireButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true
) {
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }

    WireFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.buttonHeight)
            .clickable(enabled = enabled, onClick = onClick),
        accent = accent && enabled,
        cornerBrackets = false
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(Sizes.buttonHeight),
            contentAlignment = Alignment.Center
        ) {
            LabelText(text = text, color = textColor)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WireButtonPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WireButton(text = "sign_in()", onClick = {}, accent = true)
            WireButton(text = "sign_up()", onClick = {})
            WireButton(text = "disabled()", onClick = {}, enabled = false)
        }
    }
}