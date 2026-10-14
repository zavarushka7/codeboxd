package ru.savushkina.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onSettingsClick = onSettingsClick,
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSettingsClick: () -> Unit = {},
) {
    TerminalScreen(topPadding = Spacing.xxl) {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// профиль",
                right = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(Sizes.iconMd)
                            .clickable(onClick = onSettingsClick)
                    )
                }
            )
            HorizontalLine()
            Spacer(Modifier.height(Spacing.lg))

            when (state) {
                is ProfileUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is ProfileUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileBody(user = state.user)
                }
            }
        }
    }
}

@Composable
private fun ProfileBody(
    user: User
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WireAvatar(
                avatarBase64 = user.avatarBase64,
                fallbackText = user.initials,
                size = 120.dp
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )


            user.role?.let { role ->
                if (role != Role.DO_NOT_SPECIFY) {
                    Spacer(Modifier.height(Spacing.md))
                    KeyValueRow(key = "role", value = role.displayName, accent = true)
                }

            }

            if (user.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.md))
                WireFrame(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        LabelText(text = "about")
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = user.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }


        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProfilePreview() {
    CodeboxTheme {
        ProfileContent(
            state = ProfileUiState.Success(
                User(
                    uid = "u1",
                    email = "alena@codebox.app",
                    name = "Алена",
                    description = "Пишу на Kotlin.",
                    role = Role.DEVELOPMENT
                )
            )
        )
    }
}