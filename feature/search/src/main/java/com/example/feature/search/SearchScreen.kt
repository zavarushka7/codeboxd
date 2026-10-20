package com.example.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun SearchScreen(
    onItemClick: (String) -> Unit,
    onSearchByTypeClick: () -> Unit,
    onTopRatedClick: () -> Unit,
    onLowestRatedClick: () -> Unit,
    onUserSearchClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        query = query,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = { viewModel.onQueryChange("") },
        onItemClick = onItemClick,
        onSearchByTypeClick = onSearchByTypeClick,
        onTopRatedClick = onTopRatedClick,
        onLowestRatedClick = onLowestRatedClick,
        onUserSearchClick = onUserSearchClick
    )
}

@Composable
fun SearchContent(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit = {},
    onClearQuery: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onSearchByTypeClick: () -> Unit = {},
    onTopRatedClick: () -> Unit = {},
    onLowestRatedClick: () -> Unit = {},
    onUserSearchClick: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(text = "поиск")
            Spacer(Modifier.height(Spacing.lg))
            HorizontalLine()
            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    WireTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = "имя айтема",
                        placeholder = "начните вводить"
                    )
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .size(Sizes.inputHeight)
                            .clickable(onClick = onClearQuery),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Сбросить поиск",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Sizes.iconMd)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            if (query.isBlank()) {
                FilterOptionButton("// поиск по типу", onSearchByTypeClick)
                Spacer(Modifier.height(Spacing.sm))
                FilterOptionButton("// с наибольшим рейтингом", onTopRatedClick)
                Spacer(Modifier.height(Spacing.sm))
                FilterOptionButton("// с наименьшим рейтингом", onLowestRatedClick)
                Spacer(Modifier.height(Spacing.sm))
                FilterOptionButton("// поиск людей", onUserSearchClick)
                Spacer(Modifier.height(Spacing.md))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is SearchUiState.Idle -> Unit
                    is SearchUiState.Loading -> {
                        SkeletonList(count = 6) {
                            SkeletonItemRow()
                        }
                    }
                    is SearchUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AlertBox(text = state.message)
                        }
                    }
                    is SearchUiState.Success -> {
                        if (state.items.isEmpty()) {
                            LabelText(text = "// ничего не найдено")
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
}

@Composable
private fun FilterOptionButton(text: String, onClick: () -> Unit) {
    WireFrame(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabelText(text = text, color = MaterialTheme.colorScheme.primary)
            LabelText(text = ">", color = MaterialTheme.colorScheme.primary)
        }
    }
}