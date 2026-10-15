package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item

@Composable
fun ItemRow(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireFrame(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка/символ/буквы
            WireFrame(
                modifier = Modifier.size(Sizes.iconFrame),
                cornerBrackets = false
            ) {
                Box(
                    modifier = Modifier.size(Sizes.iconFrame),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !item.symbol.isNullOrBlank() -> Text(
                            text = item.symbol!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        !item.iconKey.isNullOrBlank() -> IconKey(
                            iconKey = item.iconKey!!
                        )
                        else -> Text(
                            text = item.fallbackText,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.type.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    LabelText(text = item.type)
                }
            }

            if (item.averageRating > 0) {
                Spacer(Modifier.width(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(
                        text = "%.1f".format(item.averageRating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}