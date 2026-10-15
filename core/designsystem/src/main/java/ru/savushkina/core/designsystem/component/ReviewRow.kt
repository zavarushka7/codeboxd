package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review

@Composable
fun ReviewRow(
    review: Review,
    isMine: Boolean = false,
    myReaction: Reaction? = null,
    onLike: () -> Unit = {},
    onDislike: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WireFrame(
        modifier = modifier.fillMaxWidth(),
        // моя рамка — акцентная, чужая — серая
        accent = isMine
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick)
                .padding(Spacing.md)
        ) {
            // ── Автор + звёзды ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable(onClick = onAuthorClick)) {
                    WireAvatar(
                        avatarBase64 = review.authorAvatarBase64,
                        fallbackText = review.authorName.take(2).uppercase().ifBlank { "??" },
                        size = 32.dp
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = review.authorName.ifBlank { "аноним" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick)
                )
                RatingStars(rating = review.rating, size = 16)
            }

            if (review.hasComment) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ReactionButton(
                    icon = if (myReaction == Reaction.LIKE)
                        Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    count = review.likes,
                    active = myReaction == Reaction.LIKE,
                    onClick = onLike
                )
                Spacer(Modifier.width(Spacing.md))
                ReactionButton(
                    icon = if (myReaction == Reaction.DISLIKE)
                        Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    count = review.dislikes,
                    active = myReaction == Reaction.DISLIKE,
                    onClick = onDislike
                )
                Spacer(Modifier.width(Spacing.md))
                LabelText(
                    text = "комментарии",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onCommentsClick)
                )
            }
        }
    }
}

@Composable
private fun ReactionButton(
    icon: ImageVector,
    count: Int,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}