package ru.savushkina.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onSignOut = {
            viewModel.signOut()
            onSignedOut()
        }
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSignOut: () -> Unit = {}
) {
    TerminalScreen {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SectionHeader(text = "user_profile")
            Spacer(Modifier.height(Spacing.xl))

            when (state) {
                is ProfileUiState.Loading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }

                is ProfileUiState.Error -> {
                    AlertBox(text = state.message)
                }

                is ProfileUiState.Success -> {
                    val user = state.user

                    // Блок с информацией о пользователе
                    WireFrame(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            KeyValueRow(key = "name", value = user.name)
                            DottedDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                            KeyValueRow(key = "email", value = user.email)
                            DottedDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                            KeyValueRow(
                                key = "role",
                                value = user.role?.displayName ?: "NOT_SET",
                                accent = user.role != null // Акцентный цвет, если роль выбрана
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.xxl))

                    // Кнопка выхода
                    WireButton(
                        text = "sign_out()",
                        onClick = onSignOut,
                        accent = false // Обычный серый цвет, так как это не основное действие
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProfilePreviewSuccess() {
    CodeboxTheme {
        ProfileContent(
            state = ProfileUiState.Success(
                User(
                    uid = "abc123",
                    email = "alena@codebox.app",
                    name = "Алена",
                    role = Role.DEVELOPMENT,
                    onboardingCompleted = true
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProfilePreviewError() {
    CodeboxTheme {
        ProfileContent(state = ProfileUiState.Error("user_not_found"))
    }
}