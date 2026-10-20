package ru.savushkina.feature.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.SectionHeader
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.FeedItem
import ru.savushkina.core.model.FeedItemType
import ru.savushkina.core.model.Reaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    onUserClick: (String) -> Unit,
    onReviewClick: (reviewId: String, commentId: String?) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(text = "активность")
            Spacer(Modifier.height(Spacing.lg))
            HorizontalLine()
            Spacer(Modifier.height(Spacing.md))

            when (val s = state) {
                is ActivityUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is ActivityUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = s.message)
                    }
                }
                is ActivityUiState.Success -> {
                    if (s.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока тихо")
                        }
                    } else {
                        ActivityList(
                            state = s,
                            onUserClick = onUserClick,
                            onReviewClick = onReviewClick,
                            onLoadMore = viewModel::loadMore,
                            onRetryLoadMore = viewModel::retryLoadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityList(
    state: ActivityUiState.Success,
    onUserClick: (String) -> Unit,
    onReviewClick: (String, String?) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val currentState by rememberUpdatedState(state)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index
            val total = info.totalItemsCount
            last != null && total > 0 && last >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) {
                val s = currentState
                if (s.canLoadMore && !s.isLoadingMore && s.loadMoreError == null) {
                    currentOnLoadMore()
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(state.items, key = { it.id }) { item ->
            ActivityRow(
                item = item,
                onUserClick = onUserClick,
                onReviewClick = onReviewClick
            )
        }

        if (state.loadMoreError != null) {
            item(key = "load_more_error") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AlertBox(text = state.loadMoreError)
                    WireButton(text = "повторить", onClick = onRetryLoadMore, accent = true)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    item: FeedItem,
    onUserClick: (String) -> Unit,
    onReviewClick: (String, String?) -> Unit
) {
    val actionText = when (item.type) {
        FeedItemType.NEW_FOLLOWER -> "подписался на вас"
        FeedItemType.REVIEW_REACTION ->
            if (item.reaction == Reaction.LIKE) "лайкнул ваш отзыв"
            else "дизлайкнул ваш отзыв"
        FeedItemType.REVIEW_COMMENT -> "оставил комментарий"
        FeedItemType.FOLLOWING_REVIEW -> "оставил новый отзыв"
        FeedItemType.FOLLOWING_COMMENT -> "оставил комментарий"
    }

    WireFrame(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.clickable { onUserClick(item.actorUid) }) {
                WireAvatar(
                    avatarBase64 = item.actorAvatarBase64,
                    fallbackText = item.actorName.take(2).uppercase().ifBlank { "??" },
                    size = 36.dp
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.actorName.ifBlank { "аноним" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onUserClick(item.actorUid) }
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (item.type) {
                    FeedItemType.REVIEW_REACTION -> {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "«${item.reviewComment.ifBlank { item.itemName }}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable {
                                onReviewClick(item.reviewId, null)
                            }
                        )
                    }
                    FeedItemType.REVIEW_COMMENT,
                    FeedItemType.FOLLOWING_COMMENT -> {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "«${item.commentText}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable {
                                // sourceId в feedItemId — это commentId (см. FeedWriter в ReviewRepositoryImpl.addComment).
                                onReviewClick(item.reviewId, item.sourceCommentId())
                            }
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                        LabelText(
                            text = "к отзыву: «${item.reviewComment.take(60)}»",
                            modifier = Modifier.clickable {
                                onReviewClick(item.reviewId, null)
                            }
                        )
                    }
                    FeedItemType.FOLLOWING_REVIEW -> {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "★ ${item.rating} · «${item.reviewComment.take(80)}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable {
                                onReviewClick(item.reviewId, null)
                            }
                        )
                    }
                    FeedItemType.NEW_FOLLOWER -> Unit
                }

                Spacer(Modifier.height(Spacing.xxs))
                LabelText(text = formatTime(item.timestampMillis))
            }
        }
    }
}

/**
 * `feedItemId` = "review_comment_{commentId}_{ownerUid}" — вытаскиваем commentId.
 * Если формат не совпадает (старые записи), возвращаем null — откроется просто отзыв.
 */
private fun FeedItem.sourceCommentId(): String? {
    if (type != FeedItemType.REVIEW_COMMENT) return null
    // id: "${type.name.lowercase()}_${sourceId}_$ownerUid"
    val prefix = "${type.name.lowercase()}_"
    if (!id.startsWith(prefix)) return null
    val rest = id.removePrefix(prefix)
    val idx = rest.lastIndexOf('_')
    if (idx <= 0) return null
    return rest.substring(0, idx)
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "недавно"
    return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(millis))
}