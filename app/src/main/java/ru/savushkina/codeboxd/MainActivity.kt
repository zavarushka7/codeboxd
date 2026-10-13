package ru.savushkina.codeboxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.savushkina.core.data.AuthRepository
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.feature.auth.AuthScreen
import ru.savushkina.feature.onboarding.OnboardingScreen
import ru.savushkina.feature.profile.ProfileScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeboxTheme {
                var screen by remember {
                    mutableStateOf(if (authRepository.isLoggedIn) Screen.PROFILE else Screen.AUTH)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)

                ) {
                    when (screen) {
                        Screen.AUTH -> AuthScreen(
                            onLoginSuccess = { onboardingCompleted ->
                                screen =
                                    if (onboardingCompleted) Screen.PROFILE else Screen.ONBOARDING
                            },
                            onRegisterSuccess = { screen = Screen.ONBOARDING }
                        )

                        Screen.ONBOARDING -> OnboardingScreen(
                            onComplete = { screen = Screen.PROFILE }
                        )

                        Screen.PROFILE -> ProfileScreen(
                            onSignedOut = { screen = Screen.AUTH }
                        )
                    }
                }
            }
        }
    }

    private enum class Screen { AUTH, ONBOARDING, PROFILE }
}