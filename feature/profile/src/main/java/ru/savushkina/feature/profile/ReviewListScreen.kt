package ru.savushkina.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.ReviewRow
import ru.savushkina.core.designsystem.component.SkeletonReviewRow
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Review

@Composable
fun ReviewListScreen(
    onBack: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    onAuthorClick: (userId: String) -> Unit,
    currentUserId: String?,
    viewModel: ReviewListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// отзывы",
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

            when (val s = state) {
                is ReviewListUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        repeat(4) { SkeletonReviewRow() }
                    }
                }
                is ReviewListUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            AlertBox(text = s.message)
                            WireButton(
                                text = "повторить",
                                onClick = { /* retry через пересоздание — не нужен */ },
                                enabled = false
                            )
                        }
                    }
                }
                is ReviewListUiState.Success -> {
                    if (s.reviews.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока нет отзывов")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Spacing.xxl),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(s.reviews, key = Review::reviewId) { review ->
                                val isMine = currentUserId != null &&
                                        review.userId == currentUserId
                                WireFrame(modifier = Modifier.fillMaxWidth()) {
                                    ReviewRow(
                                        review = review,
                                        isMine = isMine,
                                        onCardClick = { onReviewClick(review.reviewId) },
                                        onCommentsClick = { onReviewClick(review.reviewId) },
                                        onAuthorClick = { onAuthorClick(review.userId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}