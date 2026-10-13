package ru.savushkina.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun AuthScreen(
    onLoginSuccess: (Boolean) -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onSignIn = { viewModel.signIn(onLoginSuccess) },
        onSignUp = { viewModel.signUp(onRegisterSuccess) }
    )
}

@Composable
fun AuthContent(
    state: AuthState,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePassword: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignUp: () -> Unit = {}
) {
    TerminalScreen {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            SectionHeader(text = "codebox")
            Spacer(Modifier.height(Spacing.xxl))

            WireTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "email",

            )
            Spacer(Modifier.height(Spacing.md))

            WireTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "password",
                placeholder = "••••••••",
                visualTransformation = if (state.isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        imageVector = if (state.isPasswordVisible)
                            Icons.Outlined.Visibility
                        else
                            Icons.Outlined.VisibilityOff,
                        contentDescription = if (state.isPasswordVisible)
                            "Скрыть пароль"
                        else
                            "Показать пароль",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(Sizes.iconMd)
                            .clickable(onClick = onTogglePassword)
                    )
                }
            )

            // Ошибка отображается через AlertBox (без красного цвета, только акцентная рамка)
            state.error?.let {
                Spacer(Modifier.height(Spacing.md))
                AlertBox(text = it)
            }

            Spacer(Modifier.height(Spacing.xl))

            // Кнопка входа — акцентная (зелёная рамка и текст)
            WireButton(
                text = if (state.isLoading) "загрузка..." else "войти_в_аккаунт",
                onClick = onSignIn,
                accent = true,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(Spacing.sm))

            // Кнопка регистрации — обычная (серая рамка)
            WireButton(
                text = "зарегистрироваться",
                onClick = onSignUp,
                enabled = !state.isLoading
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AuthPreviewEmpty() {
    CodeboxTheme {
        AuthContent(state = AuthState())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AuthPreviewError() {
    CodeboxTheme {
        AuthContent(
            state = AuthState(
                email = "dev@codebox.app",
                password = "wrong",
                error = "invalid credentials"
            )
        )
    }
}