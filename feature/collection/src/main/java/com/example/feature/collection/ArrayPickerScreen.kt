package com.example.feature.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.UserCollection

@Composable
fun ArrayPickerScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: ArrayPickerViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    ArrayPickerContent(
        query = query,
        selectedIds = selectedIds,
        allowedType = viewModel.typeName,
        isSaving = isSaving,
        error = error,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onToggleItem = viewModel::toggleItem,
        onSubmit = { viewModel.submit(onCreated) },
        onBack = onBack
    )
}

@Composable
fun ArrayPickerContent(
    query: String,
    selectedIds: List<String>,
    allowedType: String,
    isSaving: Boolean,
    error: String?,
    state: ArrayPickerUiState,
    onQueryChange: (String) -> Unit = {},
    onToggleItem: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// выбор ($allowedType)",
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

            LabelText(
                text = "выбрано: ${selectedIds.size}",
                color = if (selectedIds.size >= UserCollection.MIN_SIZE)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))

            WireTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "поиск",
                placeholder = "начните вводить"
            )

            Spacer(Modifier.height(Spacing.md))

            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    is ArrayPickerUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BlinkingCursor()
                        }
                    }
                    is ArrayPickerUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AlertBox(text = state.message)
                        }
                    }
                    is ArrayPickerUiState.Success -> {
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
                                    val isSelected = selectedIds.contains(item.id)
                                    ArrayPickerItemRow(
                                        item = item,
                                        isSelected = isSelected,
                                        onClick = { onToggleItem(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(Spacing.md))
                AlertBox(text = it)
            }

            Spacer(Modifier.height(Spacing.md))

            WireButton(
                text = if (isSaving) "создание..." else "готово",
                onClick = onSubmit,
                accent = true,
                enabled = !isSaving && selectedIds.size >= UserCollection.MIN_SIZE
            )
        }
    }
}

@Composable
private fun ArrayPickerItemRow(
    item: Item,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    WireFrame(
        modifier = Modifier.fillMaxWidth(),
        accent = isSelected
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.type.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    LabelText(text = item.type)
                }
            }
            LabelText(
                text = if (isSelected) "[x]" else "[ ]",
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

