package ru.savushkina.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onSignIn = { viewModel.signIn(onAuthSuccess) },
        onSignUp = { viewModel.signUp(onAuthSuccess) }
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
        // Показываем загрузку при isLoading, а также после успеха (isDone),
        // чтобы форма не мелькнула до переключения SessionState.
        if (state.isLoading || state.isDone) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BlinkingCursor()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .padding(vertical = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionHeader(text = "codebox")
                    Spacer(Modifier.height(Spacing.xxl))

                    WireTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "email",
                        enabled = !state.isLoading
                    )

                    Spacer(Modifier.height(Spacing.md))

                    WireTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "password",
                        visualTransformation = if (state.isPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            PasswordVisibilityToggle(
                                visible = state.isPasswordVisible,
                                onToggle = onTogglePassword
                            )
                        }
                    )

                    state.error?.let {
                        Spacer(Modifier.height(Spacing.md))
                        AlertBox(text = it)
                    }

                    Spacer(Modifier.height(Spacing.xl))

                    WireButton(
                        text = "войти",
                        onClick = onSignIn,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    WireButton(
                        text = "зарегистрироваться",
                        onClick = onSignUp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AuthPreviewEmpty() {
    CodeboxTheme { AuthContent(state = AuthState()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AuthPreviewFilled() {
    CodeboxTheme {
        AuthContent(
            state = AuthState(
                email = "dev@codebox.app",
                password = "secret123"
            )
        )
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
                error = "Неверный email или пароль"
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AuthPreviewLoading() {
    CodeboxTheme {
        AuthContent(
            state = AuthState(
                email = "dev@codebox.app",
                password = "secret123",
                isLoading = true
            )
        )
    }
}