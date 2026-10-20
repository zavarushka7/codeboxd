package ru.savushkina.feature.review

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Reaction

@Composable
fun ReviewReactionsScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ReviewReactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// реакции",
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

            when (val s = state) {
                is ReactionsUiState.Loading -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { BlinkingCursor() }
                is ReactionsUiState.Error -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        AlertBox(text = s.message)
                        WireButton(text = "повторить", onClick = viewModel::retry, accent = true)
                    }
                }
                is ReactionsUiState.Success -> {
                    if (s.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока нет реакций")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(s.items, key = { it.user.uid + it.reaction.name }) { entry ->
                                WireFrame(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onUserClick(entry.user.uid) }
                                            .padding(Spacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        WireAvatar(
                                            avatarBase64 = entry.user.avatarBase64,
                                            fallbackText = entry.user.initials,
                                            size = 36.dp
                                        )
                                        Spacer(Modifier.width(Spacing.sm))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.user.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            if (entry.user.handle.isNotBlank()) {
                                                LabelText(
                                                    text = entry.user.handle,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (entry.reaction == Reaction.LIKE) "👍" else "👎",
                                            style = MaterialTheme.typography.titleMedium
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