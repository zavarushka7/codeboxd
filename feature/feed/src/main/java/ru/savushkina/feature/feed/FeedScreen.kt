package ru.savushkina.feature.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item
import ru.savushkina.feature.suggestion.SuggestionDialog

/**
 * Отступ снизу для последнего элемента LazyColumn, чтобы FAB (56dp)
 * с отступом Spacing.lg (16dp) и небольшим запасом не перекрывал контент.
 * 56 + 16 + 16 = 88dp.
 */
private val FAB_CLEARANCE = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onItemClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showSuggestion by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        FeedContent(
            state = state,
            isRefreshing = isRefreshing,
            onItemClick = onItemClick,
            onRetry = viewModel::retry,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetryLoadMore = viewModel::retryLoadMore
        )

        FloatingActionButton(
            onClick = { showSuggestion = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Предложить айтем"
            )
        }
    }

    if (showSuggestion) {
        SuggestionDialog(
            onDismiss = { showSuggestion = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    state: FeedUiState,
    isRefreshing: Boolean = false,
    onItemClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
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
                    SkeletonList(count = 8) {
                        SkeletonItemRow()
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
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
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

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val total = layoutInfo.totalItemsCount
            if (lastVisible == null || total == 0) false
            else lastVisible >= total - 3
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
        // ВАЖНО: bottom = FAB_CLEARANCE (88dp), чтобы FAB не перекрывал
        // последний элемент и кнопку «повторить» при ошибке loadMore.
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(state.items, key = { it.id }) { item ->
            ItemRow(item = item, onClick = { onItemClick(item.id) })
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