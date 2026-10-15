package ru.savushkina.feature.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewContent(
        state = state,
        onBack = onBack,
        onRatingChange = viewModel::onRatingChange,
        onCommentChange = viewModel::onCommentChange,
        onSave = { viewModel.save(onSuccess = onSaved) }
    )
}

@Composable
fun ReviewContent(
    state: ReviewFormState,
    onBack: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
    onCommentChange: (String) -> Unit = {},
    onSave: () -> Unit = {}
) {
    TerminalScreen {
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

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlinkingCursor()
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                state.item?.let { item ->
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.type.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.xxs))
                        LabelText(text = item.type)
                    }
                }

                Spacer(Modifier.height(Spacing.lg))

                LabelText(text = "// ваша оценка")
                Spacer(Modifier.height(Spacing.sm))
                RatingStars(
                    rating = state.rating,
                    onRatingChange = onRatingChange,
                    size = 32
                )

                Spacer(Modifier.height(Spacing.lg))

                WireTextField(
                    value = state.comment,
                    onValueChange = onCommentChange,
                    label = "комментарий",
                    placeholder = "поделитесь впечатлением",
                    singleLine = false,
                    enabled = !state.isSaving
                )

                state.error?.let {
                    Spacer(Modifier.height(Spacing.md))
                    AlertBox(text = it)
                }

                Spacer(Modifier.height(Spacing.xl))

                WireButton(
                    text = if (state.isSaving) "сохранение..." else "сохранить",
                    onClick = onSave,
                    accent = true,
                    enabled = !state.isSaving
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ReviewPreview() {
    CodeboxTheme {
        ReviewContent(
            state = ReviewFormState(
                item = ru.savushkina.core.model.Item(
                    id = "1", name = "Kotlin", type = "Язык", symbol = "KT"
                ),
                rating = 4,
                comment = "Отличный язык"
            )
        )
    }
}