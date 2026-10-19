package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.savushkina.core.model.ItemConstraints

@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    size: Int = 24
) {
    Row(modifier = modifier) {
        (1..ItemConstraints.RATING_MAX).forEach { star ->
            val filled = star <= rating
            val starModifier = if (onRatingChange != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { onRatingChange(star) }
            } else Modifier

            Box(
                modifier = starModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filled) "★" else "☆",
                    color = if (filled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    fontSize = size.sp
                )
            }
        }
    }
}