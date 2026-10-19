package com.example.feature.favourite_picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun FavouritePickerScreen(
    onBack: () -> Unit,
    onItemSelected: (String) -> Unit,
    viewModel: FavouritePickerViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favouritesLoadError by viewModel.favouritesLoadError.collectAsStateWithLifecycle()

    FavouritePickerContent(
        query = query,
        state = state,
        favouritesLoadError = favouritesLoadError,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onItemSelected = onItemSelected
    )
}

@Composable
fun FavouritePickerContent(
    query: String,
    state: FavouritePickerUiState,
    favouritesLoadError: String? = null,
    onQueryChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onItemSelected: (String) -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// выбрать айтем",
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

            favouritesLoadError?.let {
                Spacer(Modifier.height(Spacing.md))
                AlertBox(text = it)
            }

            Spacer(Modifier.height(Spacing.md))

            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is FavouritePickerUiState.Idle,
                    is FavouritePickerUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BlinkingCursor()
                        }
                    }
                    is FavouritePickerUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AlertBox(text = state.message)
                        }
                    }
                    is FavouritePickerUiState.Success -> {
                        if (state.items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LabelText(
                                    text = if (query.isBlank())
                                        "// все айтемы уже в четвёрке"
                                    else
                                        "// ничего не найдено"
                                )
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