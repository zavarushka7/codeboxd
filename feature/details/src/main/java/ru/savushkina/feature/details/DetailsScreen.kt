package ru.savushkina.feature.details

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.IconKey
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.RatingStars
import ru.savushkina.core.designsystem.component.ReviewRow
import ru.savushkina.core.designsystem.component.SectionHeader
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Reaction
import ru.savushkina.core.model.Review

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    onAuthorClick: (userId: String) -> Unit,
    currentUserId: String?,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val myReactions by viewModel.myReactions.collectAsStateWithLifecycle()

    DetailsContent(
        state = state,
        myReactions = myReactions,
        currentUserId = currentUserId,
        onBack = onBack,
        onWriteReview = onWriteReview,
        onReviewClick = onReviewClick,
        onAuthorClick = onAuthorClick,
        onLike = viewModel::onLike,
        onDislike = viewModel::onDislike
    )
}

@Composable
fun DetailsContent(
    state: DetailsUiState,
    myReactions: Map<String, Reaction?> = emptyMap(),
    currentUserId: String? = null,
    onBack: () -> Unit = {},
    onWriteReview: () -> Unit = {},
    onReviewClick: (reviewId: String) -> Unit = {},
    onAuthorClick: (userId: String) -> Unit = {},
    onLike: (String) -> Unit = {},
    onDislike: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// item",
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

            when (state) {
                is DetailsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is DetailsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is DetailsUiState.Success -> {
                    DetailsBody(
                        item = state.item,
                        reviews = state.reviews,
                        myReview = state.myReview,
                        myReactions = myReactions,
                        currentUserId = currentUserId,
                        onWriteReview = onWriteReview,
                        onReviewClick = onReviewClick,
                        onAuthorClick = onAuthorClick,
                        onLike = onLike,
                        onDislike = onDislike
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsBody(
    item: Item,
    reviews: List<Review>,
    myReview: Review?,
    myReactions: Map<String, Reaction?>,
    currentUserId: String?,
    onWriteReview: () -> Unit,
    onReviewClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onLike: (String) -> Unit,
    onDislike: (String) -> Unit
) {
    val symbol = item.symbol
    val iconKey = item.iconKey

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // ── Заголовок айтема ──
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WireFrame(modifier = Modifier.size(72.dp), accent = true) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        when {
                            !symbol.isNullOrBlank() -> Text(
                                text = symbol,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            !iconKey.isNullOrBlank() -> IconKey(
                                iconKey = iconKey,
                                modifier = Modifier.size(Sizes.iconLg)
                            )
                            else -> Text(
                                text = item.fallbackText,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.width(Spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.type.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.xxs))
                        LabelText(text = item.type)
                    }
                    if (item.averageRating > 0) {
                        Spacer(Modifier.height(Spacing.xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingStars(rating = item.averageRating.toInt(), size = 16)
                            Spacer(Modifier.width(Spacing.xs))
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

        // ── Описание ──
        if (item.description.isNotBlank()) {
            item(key = "description") {
                WireFrame(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        LabelText(text = "description")
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // ── Кнопка «оценить / изменить» ──
        item(key = "write_review") {
            if (myReview != null) {
                WireButton(text = "изменить мой отзыв", onClick = onWriteReview)
            } else {
                WireButton(text = "оценить", onClick = onWriteReview, accent = true)
            }
        }

        // ── Заголовок отзывов ──
        item(key = "reviews_header") {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(text = "отзывы")
        }

        if (reviews.isEmpty()) {
            item(key = "reviews_empty") {
                LabelText(text = "// пока нет отзывов")
            }
        } else {
            items(reviews, key = { it.reviewId }) { review ->
                val isMine = currentUserId != null && review.userId == currentUserId
                ReviewRow(
                    review = review,
                    isMine = isMine,
                    myReaction = myReactions[review.reviewId],
                    onLike = { onLike(review.reviewId) },
                    onDislike = { onDislike(review.reviewId) },
                    onCardClick = { onReviewClick(review.reviewId) },
                    onCommentsClick = { onReviewClick(review.reviewId) },
                    onAuthorClick = { onAuthorClick(review.userId) }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailsPreview() {
    CodeboxTheme {
        DetailsContent(
            state = DetailsUiState.Success(
                item = Item(
                    id = "1", name = "Kotlin", type = "Язык",
                    description = "Статически типизированный язык",
                    symbol = "KT",
                    averageRating = 4.5
                ),
                reviews = listOf(
                    Review(
                        userId = "u1", itemId = "1", rating = 5,
                        comment = "Отличный язык!",
                        likes = 12, dislikes = 1,
                        authorName = "Алена",
                        itemName = "Kotlin"
                    )
                ),
                myReview = null
            )
        )
    }
}