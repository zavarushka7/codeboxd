package com.example.feature.search

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.SkeletonItemRow
import ru.savushkina.core.designsystem.component.SkeletonList
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.User

@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// поиск людей",
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
                onValueChange = viewModel::onQueryChange,
                label = "username",
                placeholder = "@handle"
            )

            Spacer(Modifier.height(Spacing.md))

            when (state) {
                is UserSearchUiState.Idle -> {
                    LabelText(text = "// введите username")
                }
                is UserSearchUiState.Loading -> {
                    SkeletonList(count = 4) { SkeletonItemRow() }
                }
                is UserSearchUiState.Error -> {
                    AlertBox(text = (state as UserSearchUiState.Error).message)
                }
                is UserSearchUiState.Success -> {
                    val users = (state as UserSearchUiState.Success).users
                    if (users.isEmpty()) {
                        LabelText(text = "// никого не нашли")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(users, key = { it.uid }) { user ->
                                UserRow(user = user, onClick = { onUserClick(user.uid) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: User, onClick: () -> Unit) {
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
            Spacer(Modifier.padding(start = Spacing.md))
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