package ru.savushkina.feature.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
    FeedContent(
        state = state,
        onItemClick = onItemClick,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore
    )
}

@Composable
fun FeedContent(
    state: FeedUiState,
    onItemClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRetryLoadMore: () -> Unit = {}
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            AlertBox(text = state.message)
                            WireButton(
                                text = "повторить",
                                onClick = onRetry,
                                accent = true
                            )
                        }
                    }
                }
                is FeedUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока нет айтемов")
                        }
                    } else {
                        FeedList(
                            state = state,
                            onItemClick = onItemClick,
                            onLoadMore = onLoadMore,
                            onRetryLoadMore = onRetryLoadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedList(
    state: FeedUiState.Success,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentState by rememberUpdatedState(state)

    // Автоподгрузка: snapshotFlow подписывается на изменения visibleItemsInfo
    // и эмитит, когда пользователь доскроллил до конца.
    //
    // Раньше был LaunchedEffect с ключами (canLoadMore, isLoadingMore, loadMoreError)
    // — он не реагировал на скролл, потому что layoutInfo не триггерит рекомпозицию.
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val total = layoutInfo.totalItemsCount
            if (lastVisible == null || total == 0) {
                false
            } else {
                lastVisible >= total - 3
            }
        }
            .collect { nearEnd ->
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
            ItemRow(item = item, onClick = { onItemClick(item.id) })
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    BlinkingCursor()
                }
            }
        }

        state.loadMoreError?.let { error ->
            item(key = "load_more_error") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AlertBox(text = error)
                    WireButton(
                        text = "повторить",
                        onClick = onRetryLoadMore,
                        accent = true
                    )
                }
            }
        }

        if (!state.canLoadMore && state.items.isNotEmpty() && state.loadMoreError == null) {
            item(key = "end_of_feed") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    LabelText(text = "// конец ленты")
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