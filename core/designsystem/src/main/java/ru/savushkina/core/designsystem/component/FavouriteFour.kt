package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item

@Composable
fun FavouriteFourSection(
    favourites: List<Item>,
    isOwnProfile: Boolean,
    onSlotClick: (Int) -> Unit,
    onRemoveFavourite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LabelText(text = "// любимая четвёрка")
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            for (index in 0 until 4) {
                val item = favourites.getOrNull(index)

                FavouriteSlot(
                    item = item,
                    isOwnProfile = isOwnProfile,
                    onClick = { if (item == null) onSlotClick(index) },
                    onRemove = { item?.let { onRemoveFavourite(it.id) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FavouriteSlot(
    item: Item?,
    isOwnProfile: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireFrame(
        modifier = modifier
            .height(90.dp)
            .clickable(
                enabled = isOwnProfile && item == null,
                onClick = onClick
            ),
        accent = item != null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item == null) {
                if (isOwnProfile) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Добавить",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                } else {
                    LabelText(
                        text = "—",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val symbol = item.symbol
                        val iconKey = item.iconKey
                        when {
                            !symbol.isNullOrBlank() -> Text(
                                text = symbol,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            !iconKey.isNullOrBlank() -> IconKey(
                                iconKey = iconKey,
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Text(
                                text = item.fallbackText,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.xxs))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

                if (isOwnProfile) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onRemove),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Убрать из избранного",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}