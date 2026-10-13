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
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    big: Boolean = false
) {
    WireFrame(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            LabelText(text = label)
            Spacer(Modifier.height(Spacing.xs))
            ValueText(text = value, big = big)
            if (hint != null) {
                Spacer(Modifier.height(Spacing.xxs))
                LabelText(text = hint)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetricCardPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "temperature",
                value = "24°C",
                big = true,
                hint = "feels like 22°C"
            )
            MetricCard(
                label = "total value",
                value = "$ 236,780,340"
            )
        }
    }
}