package ru.savushkina.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.PasswordVisibilityToggle
import ru.savushkina.core.designsystem.component.SectionHeader
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ВАЖНО: isDone — это СОБЫТИЕ «форма обработана, можно уходить».
    // Оно должно сбрасываться, когда AuthScreen уходит из композиции
    // (навигация по navTarget = FEED). Иначе при повторном показе экрана
    // (deep link, back из FEED через сценарий, который оставил AUTH в стеке,
    // или пересоздание Activity с сохранённым NavBackStackEntry) UI увидит
    // isDone = true и покажет вечный спиннер.
    //
    // onDispose вызывается ровно в момент, когда composable удаляется
    // из композиции. Навигация NavHost'а удаляет AuthScreen — onDispose
    // срабатывает → isDone сбрасывается.
    DisposableEffect(viewModel) {
        onDispose { viewModel.onDoneHandled() }
    }

    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp
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
        if (state.isLoading || state.isDone) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        label = "email"
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

                    WireButton(text = "войти", onClick = onSignIn)
                    Spacer(Modifier.height(Spacing.sm))
                    WireButton(text = "зарегистрироваться", onClick = onSignUp)
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