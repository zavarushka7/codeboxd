package ru.savushkina.codeboxd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.savushkina.core.model.SessionState
import ru.savushkina.core.navigation.Routes
import ru.savushkina.feature.auth.AuthScreen
import ru.savushkina.feature.onboarding.OnboardingScreen
import ru.savushkina.feature.profile.ProfileScreen
import ru.savushkina.feature.settings.SettingsScreen

@Composable
fun CodeboxNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            SessionState.Unknown -> Unit
            SessionState.Unauthenticated -> navController.navigateSingleTop(Routes.AUTH)
            is SessionState.Onboarding -> navController.navigateSingleTop(Routes.ONBOARDING)
            is SessionState.Authenticated -> navController.navigateSingleTop(Routes.PROFILE)
        }
    }

    NavHost(navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(onAuthSuccess = { /* навигация через LaunchedEffect */ })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = { /* навигация через LaunchedEffect */ })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onSettingsClick = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.SETTINGS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}