package ru.savushkina.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item

@Composable
fun TopRatedScreen(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: TopRatedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TopRatedContent(state = state, onBack = onBack, onItemClick = onItemClick)
}

@Composable
fun TopRatedContent(
    state: TopRatedUiState,
    onBack: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// наибольший рейтинг",
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
                is TopRatedUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is TopRatedUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is TopRatedUiState.Success -> {
                    if (state.items.isEmpty()) {
                        LabelText(text = "// айтемов пока нет")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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

