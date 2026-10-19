package com.example.feature.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import ru.savushkina.core.designsystem.component.ItemRow
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun CollectionPickerScreen(
    onBack: () -> Unit,
    onItemAdded: () -> Unit,
    viewModel: CollectionPickerViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    CollectionPickerContent(
        query = query,
        state = state,
        error = error,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onItemSelected = { itemId -> viewModel.addItem(itemId, onItemAdded) }
    )
}

@Composable
fun CollectionPickerContent(
    query: String,
    state: CollectionPickerUiState,
    error: String? = null,
    onQueryChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onItemSelected: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// добавить айтем",
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

            WireTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "поиск",
                placeholder = "начните вводить"
            )

            error?.let {
                Spacer(Modifier.height(Spacing.md))
                AlertBox(text = it)
            }

            Spacer(Modifier.height(Spacing.md))

            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is CollectionPickerUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BlinkingCursor()
                        }
                    }
                    is CollectionPickerUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AlertBox(text = state.message)
                        }
                    }
                    is CollectionPickerUiState.Success -> {
                        if (state.items.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LabelText(text = "// ничего не найдено")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(state.items, key = { it.id }) { item ->
                                    ItemRow(
                                        item = item,
                                        onClick = { onItemSelected(item.id) }
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