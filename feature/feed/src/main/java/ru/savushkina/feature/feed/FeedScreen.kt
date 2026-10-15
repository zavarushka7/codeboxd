package ru.savushkina.feature.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item

@Composable
fun FeedScreen(
    onItemClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeedContent(state = state, onItemClick = onItemClick)
}

@Composable
fun FeedContent(
    state: FeedUiState,
    onItemClick: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(text = "codebox")
            Spacer(Modifier.height(Spacing.lg))
            HorizontalLine()
            Spacer(Modifier.height(Spacing.md))

            when (state) {
                is FeedUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is FeedUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is FeedUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока нет айтемов")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                ItemRow(item = item, onClick = { onItemClick(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FeedPreview() {
    CodeboxTheme {
        FeedContent(
            state = FeedUiState.Success(
                listOf(
                    Item(id = "1", name = "Kotlin", type = "Язык", description = "", averageRating = 4.5),
                    Item(id = "2", name = "Rust", type = "Язык", description = "", averageRating = 4.8),
                    Item(id = "3", name = "Docker", type = "Инструмент", description = "")
                )
            )
        )
    }
}