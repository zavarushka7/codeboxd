package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun AlertBox(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = true
) {
    WireFrame(
        modifier = modifier.fillMaxWidth(),
        accent = accent,
        cornerBrackets = true
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            LabelText(
                text = "! alert",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.xs))
            LabelText(
                text = text,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AlertBoxPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlertBox(text = "invalid credentials")
            AlertBox(text = "user_not_found")
        }
    }
}