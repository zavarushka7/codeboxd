package ru.savushkina.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.RolePicker
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onRoleSelect = viewModel::onRoleSelect,
        onComplete = viewModel::complete,
        onBack = viewModel::signOut
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingState,
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onRoleSelect: (Role) -> Unit = {},
    onComplete: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// init_profile",
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WireTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = "name",
                        placeholder = "Ваше имя"
                    )

                    Spacer(Modifier.height(Spacing.md))

                    WireTextField(
                        value = state.username,
                        onValueChange = onUsernameChange,
                        label = "username",
                        placeholder = "придумайте @handle"
                    )

                    Spacer(Modifier.height(Spacing.xs))

                    LabelText(
                        text = when {
                            state.isCheckingUsername -> "// проверяем..."
                            state.usernameAvailable == true -> "// @${state.username} свободен"
                            state.usernameAvailable == false -> "// @${state.username} занят"
                            else -> "// 3–20 символов, a-z, 0-9, _"
                        },
                        color = when {
                            state.usernameAvailable == true -> MaterialTheme.colorScheme.primary
                            state.usernameAvailable == false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(Modifier.height(Spacing.xl))

                    LabelText(
                        text = "// выберите свою роль",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(Spacing.md))

                    RolePicker(
                        selectedRole = state.role,
                        onRoleSelect = onRoleSelect
                    )

                    state.error?.let {
                        Spacer(Modifier.height(Spacing.md))
                        AlertBox(text = it)
                    }

                    Spacer(Modifier.height(Spacing.xl))

                    WireButton(
                        text = if (state.isLoading) "загрузка..." else "сохранить",
                        onClick = onComplete,
                        accent = true,
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingPreviewEmpty() {
    CodeboxTheme { OnboardingContent(state = OnboardingState()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingPreviewSelected() {
    CodeboxTheme {
        OnboardingContent(
            state = OnboardingState(name = "Алена", username = "alena", role = Role.DEVELOPMENT)
        )
    }
}