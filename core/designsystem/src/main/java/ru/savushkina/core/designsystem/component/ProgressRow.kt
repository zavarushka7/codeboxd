package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun ProgressRow(
    label: String,
    progress: Float,
    valueText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        LabelText(text = label, modifier = Modifier.width(80.dp))
        ProgressBar(progress = progress, modifier = Modifier.weight(1f))
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProgressRowPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressRow(label = "crypto", progress = 0.40f, valueText = "40%")
            ProgressRow(label = "stocks", progress = 0.25f, valueText = "25%")
            ProgressRow(label = "cash", progress = 0.05f, valueText = "5%")
        }
    }
}