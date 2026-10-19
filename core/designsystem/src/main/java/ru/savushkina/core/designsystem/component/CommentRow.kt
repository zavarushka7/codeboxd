package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.ReviewComment

@Composable
fun CommentRow(
    comment: ReviewComment,
    isMine: Boolean = false,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onAuthorClick: (userId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    WireFrame(
        modifier = modifier.fillMaxWidth(),
        accent = isMine
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.clickable {
                    comment.authorId?.let { onAuthorClick(it) }
                }
            ) {
                WireAvatar(
                    avatarBase64 = comment.authorAvatarBase64,
                    fallbackText = comment.authorName.take(2).uppercase().ifBlank { "??" },
                    size = 28.dp
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.authorName.ifBlank { "аноним" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        comment.authorId?.let { onAuthorClick(it) }
                    }
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (canDelete) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Удалить комментарий",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}