package ru.savushkina.feature.profile

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
import ru.savushkina.core.model.User

@Composable
fun FollowListScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FollowListContent(
        title = viewModel.mode.routeTitle(isOwn = viewModel.isOwnList),
        state = state,
        onBack = onBack,
        onUserClick = onUserClick,
        onRetry = viewModel::retry
    )
}

@Composable
fun FollowListContent(
    title: String,
    state: FollowListUiState,
    onBack: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
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
                is FollowListUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is FollowListUiState.Error -> {
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
                is FollowListUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LabelText(text = "// пока пусто")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(state.users, key = { it.uid }) { user ->
                                FollowUserRow(user = user, onClick = { onUserClick(user.uid) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(user: User, onClick: () -> Unit) {
    WireFrame(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WireAvatar(
                avatarBase64 = user.avatarBase64,
                fallbackText = user.initials,
                size = 40.dp
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (user.handle.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xxs))
                    LabelText(
                        text = user.handle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}