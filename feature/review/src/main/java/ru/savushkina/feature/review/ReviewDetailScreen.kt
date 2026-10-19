package ru.savushkina.feature.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.CommentRow
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.RatingStars
import ru.savushkina.core.designsystem.component.SectionHeader
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.ReviewComment

@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    onAuthorClick: (userId: String) -> Unit,
    currentUserId: String?,
    viewModel: ReviewDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentsLoaded by viewModel.commentsLoaded.collectAsStateWithLifecycle()
    val draft by viewModel.commentDraft.collectAsStateWithLifecycle()
    val isSending by viewModel.isSendingComment.collectAsStateWithLifecycle()
    val canLoadMore by viewModel.canLoadMoreComments.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMoreComments.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    ReviewDetailContent(
        state = state,
        comments = comments,
        commentsLoaded = commentsLoaded,
        commentDraft = draft,
        isSendingComment = isSending,
        canLoadMoreComments = canLoadMore,
        isLoadingMoreComments = isLoadingMore,
        currentUserId = currentUserId,
        error = error,
        onBack = onBack,
        onAuthorClick = onAuthorClick,
        onCommentDraftChange = viewModel::onCommentDraftChange,
        onSubmitComment = { onDone -> viewModel.submitComment(onDone) },
        onDeleteComment = viewModel::deleteComment,
        onLoadMoreComments = viewModel::loadMoreComments
    )
}

@Composable
fun ReviewDetailContent(
    state: ReviewDetailUiState,
    comments: List<ReviewComment> = emptyList(),
    commentsLoaded: Boolean = false,
    commentDraft: String = "",
    isSendingComment: Boolean = false,
    canLoadMoreComments: Boolean = false,
    isLoadingMoreComments: Boolean = false,
    currentUserId: String? = null,
    error: String? = null,
    onBack: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onCommentDraftChange: (String) -> Unit = {},
    onSubmitComment: (onDone: () -> Unit) -> Unit = { it() },
    onDeleteComment: (String) -> Unit = {},
    onLoadMoreComments: () -> Unit = {}
) {
    // rememberSaveable — переживает поворот экрана
    var inputVisible by rememberSaveable { mutableStateOf(false) }

    TerminalScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TerminalTopBar(
                    title = "// отзыв",
                    left = {
                        LabelText(
                            text = "< назад",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onBack)
                        )
                    }
                )
                HorizontalLine()
                Spacer(Modifier.height(Spacing.md))

                val isReady = state is ReviewDetailUiState.Success && commentsLoaded

                if (!isReady) {
                    when (state) {
                        is ReviewDetailUiState.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AlertBox(text = state.message)
                            }
                        }
                        else -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                BlinkingCursor()
                            }
                        }
                    }
                } else if (state is ReviewDetailUiState.Success) {
                    val review = state.review
                    val isMine = currentUserId != null && review.userId == currentUserId

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        item(key = "review_card") {
                            WireFrame(
                                modifier = Modifier.fillMaxWidth(),
                                accent = isMine
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.clickable {
                                                onAuthorClick(review.userId)
                                            }
                                        ) {
                                            WireAvatar(
                                                avatarBase64 = review.authorAvatarBase64,
                                                fallbackText = review.authorName
                                                    .take(2).uppercase().ifBlank { "??" },
                                                size = 40.dp
                                            )
                                        }
                                        Spacer(Modifier.width(Spacing.sm))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = review.authorName.ifBlank { "аноним" },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    onAuthorClick(review.userId)
                                                }
                                            )
                                            if (review.itemName.isNotBlank()) {
                                                Spacer(Modifier.height(Spacing.xxs))
                                                LabelText(text = review.itemName)
                                            }
                                        }
                                        RatingStars(rating = review.rating, size = 16)
                                    }
                                    if (review.hasComment) {
                                        Spacer(Modifier.height(Spacing.md))
                                        Text(
                                            text = review.comment,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "comments_header") {
                            Spacer(Modifier.height(Spacing.lg))
                            SectionHeader(text = "комментарии")
                        }

                        if (comments.isEmpty()) {
                            item(key = "comments_empty") {
                                LabelText(text = "// пока нет комментариев")
                            }
                        } else {
                            items(comments, key = { it.id }) { c ->
                                val isCommentMine =
                                    currentUserId != null && c.authorId == currentUserId
                                CommentRow(
                                    comment = c,
                                    isMine = isCommentMine,
                                    canDelete = isCommentMine,
                                    onDelete = { onDeleteComment(c.id) },
                                    onAuthorClick = onAuthorClick
                                )
                            }
                        }

                        if (canLoadMoreComments) {
                            item(key = "load_more") {
                                Spacer(Modifier.height(Spacing.md))
                                WireButton(
                                    text = if (isLoadingMoreComments) "загрузка..."
                                    else "показать ещё",
                                    onClick = onLoadMoreComments,
                                    enabled = !isLoadingMoreComments
                                )
                            }
                        }
                    }
                }
            }

            if (state is ReviewDetailUiState.Success && commentsLoaded) {
                FloatingActionButton(
                    onClick = { inputVisible = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.lg)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Добавить комментарий"
                    )
                }
            }

            if (inputVisible) {
                CommentInputDialog(
                    value = commentDraft,
                    onValueChange = onCommentDraftChange,
                    onDismiss = { inputVisible = false },
                    onSubmit = { onSubmitComment { inputVisible = false } },
                    isSubmitting = isSendingComment,
                    error = error
                )
            }
        }
    }
}

@Composable
private fun CommentInputDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    error: String? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .imePadding()
        ) {
            WireFrame(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)   // ← непрозрачный фон
                        .padding(Spacing.md)
                ) {
                    LabelText(text = "// новый комментарий")
                    Spacer(Modifier.height(Spacing.md))

                    WireTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = "текст комментария",
                        singleLine = false,
                        minHeight = 140.dp,
                        enabled = !isSubmitting
                    )

                    error?.let {
                        Spacer(Modifier.height(Spacing.md))
                        AlertBox(text = it)
                    }

                    Spacer(Modifier.height(Spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            WireButton(
                                text = "отмена",
                                onClick = onDismiss,
                                enabled = !isSubmitting
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            WireButton(
                                text = if (isSubmitting) "..." else "отправить",
                                onClick = onSubmit,
                                accent = true,
                                enabled = !isSubmitting && value.isNotBlank()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ReviewDetailPreview() {
    CodeboxTheme {
        ReviewDetailContent(
            state = ReviewDetailUiState.Success(
                Review(
                    userId = "u1", itemId = "1", rating = 5,
                    comment = "Отличный язык!",
                    authorName = "Алена",
                    itemName = "Kotlin"
                )
            ),
            commentsLoaded = true
        )
    }
}