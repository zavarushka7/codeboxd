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
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.UserCollection

@Composable
fun CollectionsListScreen(
    onBack: () -> Unit,
    onCollectionClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: CollectionsListViewModel = hiltViewModel()
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val isOwn by viewModel.isOwnCollections.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    CollectionsListContent(
        collections = collections,
        isOwnCollections = isOwn,
        error = error,
        onBack = onBack,
        onCollectionClick = onCollectionClick,
        onCreateClick = onCreateClick,
        onRetry = viewModel::retry
    )
}

@Composable
fun CollectionsListContent(
    collections: List<UserCollection>,
    isOwnCollections: Boolean = true,
    error: String? = null,
    onBack: () -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = if (isOwnCollections) "// коллекции" else "// коллекции пользователя",
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

            if (isOwnCollections) {
                WireButton(
                    text = "+ создать",
                    onClick = onCreateClick,
                    accent = true
                )
                Spacer(Modifier.height(Spacing.md))
            }

            if (error != null) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        AlertBox(text = error)
                        WireButton(
                            text = "повторить",
                            onClick = onRetry,
                            accent = true
                        )
                    }
                }
                return@TerminalScreen
            }

            if (collections.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LabelText(text = "// пока нет коллекций")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(collections, key = { it.id }) { collection ->
                        CollectionRow(
                            collection = collection,
                            onClick = { onCollectionClick(collection.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(
    collection: UserCollection,
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
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(Spacing.xxs))
                LabelText(
                    text = if (collection.isFixedSize)
                        "массив · ${collection.itemIds.size}/${collection.maxSize}"
                    else
                        "список · ${collection.itemIds.size}"
                )
            }
            LabelText(text = ">", color = MaterialTheme.colorScheme.primary)
        }
    }
}