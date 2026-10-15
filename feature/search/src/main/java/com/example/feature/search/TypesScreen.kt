package ru.savushkina.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun TypesScreen(
    onBack: () -> Unit,
    onTypeClick: (String) -> Unit,
    viewModel: TypesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TypesContent(
        state = state,
        onBack = onBack,
        onTypeClick = onTypeClick
    )
}

@Composable
fun TypesContent(
    state: TypesUiState,
    onBack: () -> Unit = {},
    onTypeClick: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// типы",
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
                is TypesUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is TypesUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is TypesUiState.Success -> {
                    if (state.types.isEmpty()) {
                        LabelText(text = "// типов пока нет")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(state.types, key = { it }) { type ->
                                WireFrame(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onTypeClick(type) }
                                            .padding(Spacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LabelText(
                                            text = type,
                                            color = MaterialTheme.colorScheme.primary
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
}

