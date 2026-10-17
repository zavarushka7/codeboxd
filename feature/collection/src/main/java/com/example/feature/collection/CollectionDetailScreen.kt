package ru.savushkina.feature.collection

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item

@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val isOwn = viewModel.isOwnCollection

    CollectionDetailContent(
        state = state,
        isOwnCollection = isOwn,
        onBack = onBack,
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onRemoveItem = viewModel::removeItem,
        onDeleteClick = { if (isOwn) confirmDelete = true }
    )

    if (confirmDelete && isOwn) {
        DeleteCollectionDialog(
            onConfirm = {
                confirmDelete = false
                viewModel.deleteCollection(onDeleted)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun CollectionDetailContent(
    state: CollectionDetailUiState,
    isOwnCollection: Boolean,
    onBack: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onAddItemClick: (String) -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            val title = when (state) {
                is CollectionDetailUiState.Success -> "// ${state.collection.name}"
                else -> "// коллекция"
            }
            TerminalTopBar(
                title = title,
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
                is CollectionDetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is CollectionDetailUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is CollectionDetailUiState.Success -> {
                    val collection = state.collection
                    val canEdit = isOwnCollection && collection.isEditable

                    Column(modifier = Modifier.fillMaxSize()) {
                        LabelText(
                            text = if (collection.isFixedSize)
                                "массив · ${collection.itemIds.size}/${collection.maxSize}"
                            else
                                "список · ${collection.itemIds.size}"
                        )
                        Spacer(Modifier.height(Spacing.md))

                        if (canEdit) {
                            WireButton(
                                text = "+ добавить айтем",
                                onClick = { onAddItemClick(collection.id) },
                                accent = true
                            )
                            Spacer(Modifier.height(Spacing.md))
                        }

                        if (state.items.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LabelText(text = "// пусто")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(state.items, key = { it.id }) { item ->
                                    CollectionItemRow(
                                        item = item,
                                        isEditable = canEdit,
                                        onRemove = { onRemoveItem(item.id) },
                                        onClick = { onItemClick(item.id) }
                                    )
                                }
                            }
                        }

                        if (isOwnCollection) {
                            Spacer(Modifier.height(Spacing.md))
                            WireButton(
                                text = "удалить коллекцию",
                                onClick = onDeleteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionItemRow(
    item: Item,
    isEditable: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    WireFrame(modifier = Modifier.fillMaxWidth()) {
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
            if (isEditable) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Убрать",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onRemove)
                )
            }
        }
    }
}

@Composable
private fun DeleteCollectionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        WireFrame(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
            ) {
                LabelText(text = "// удалить коллекцию?")
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WireButton(
                            text = "отмена",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WireButton(
                            text = "удалить",
                            onClick = onConfirm,
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}