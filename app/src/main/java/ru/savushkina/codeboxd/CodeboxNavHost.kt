package ru.savushkina.codeboxd

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import ru.savushkina.core.designsystem.pattern.TerminalBottomBar

import ru.savushkina.feature.auth.AuthScreen
import ru.savushkina.feature.details.DetailsScreen
import ru.savushkina.feature.favourite_picker.FavouritePickerScreen
import ru.savushkina.feature.feed.FeedScreen
import ru.savushkina.feature.onboarding.OnboardingScreen
import ru.savushkina.feature.profile.ProfileScreen
import ru.savushkina.feature.profile.ProfileViewModel
import ru.savushkina.feature.review.ReviewDetailScreen
import ru.savushkina.feature.review.ReviewScreen
import ru.savushkina.feature.search.LowestRatedScreen
import ru.savushkina.feature.search.SearchScreen
import ru.savushkina.feature.search.TopRatedScreen
import ru.savushkina.feature.search.TypeItemsScreen
import ru.savushkina.feature.search.TypesScreen
import ru.savushkina.feature.settings.SettingsScreen

@Composable
fun CodeboxNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navTarget by sessionViewModel.navTarget.collectAsStateWithLifecycle()
    val currentUserId = sessionViewModel.currentUserId

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        ?: Routes.FEED

    LaunchedEffect(navTarget) {
        val target = navTarget ?: return@LaunchedEffect
        if (target == Routes.FEED) return@LaunchedEffect
        navController.navigateSingleTop(target)
    }

    val bottomBarRoutes = setOf(
        Routes.FEED,
        Routes.SEARCH,
        Routes.PROFILE,
        Routes.DETAILS
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TerminalBottomBar(
                    currentRoute = currentRoute.orEmpty(),
                    onFeedClick = { navController.navigateSingleTop(Routes.FEED) },
                    onSearchClick = { navController.navigateSingleTop(Routes.SEARCH) },
                    onProfileClick = { navController.navigateSingleTop(Routes.PROFILE) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.FEED,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.AUTH) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.FEED) {
                FeedScreen(
                    onItemClick = { itemId ->
                        navController.navigate(Routes.details(itemId))
                    }
                )
            }

            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { entry ->
                val itemId = entry.arguments?.getString("itemId").orEmpty()
                DetailsScreen(
                    onBack = { navController.popBackStack() },
                    onWriteReview = { navController.navigate(Routes.reviewForm(itemId)) },
                    onReviewClick = { reviewId ->
                        navController.navigate(Routes.reviewDetail(reviewId))
                    },
                    onAuthorClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    }
                )
            }

            composable(
                route = Routes.REVIEW_FORM,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) {
                ReviewScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.REVIEW_DETAIL,
                arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
            ) {
                ReviewDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onItemClick = { itemId ->
                        navController.navigate(Routes.details(itemId))
                    },
                    onSearchByTypeClick = { navController.navigate(Routes.TYPES) },
                    onTopRatedClick = { navController.navigate(Routes.TOP_RATED) },
                    onLowestRatedClick = { navController.navigate(Routes.LOWEST_RATED) }
                )
            }

            composable(Routes.TYPES) {
                TypesScreen(
                    onBack = { navController.popBackStack() },
                    onTypeClick = { type ->
                        navController.navigate(Routes.typeItems(type))
                    }
                )
            }

            composable(
                route = Routes.TYPE_ITEMS,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) {
                TypeItemsScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(Routes.details(itemId))
                    }
                )
            }

            composable(Routes.TOP_RATED) {
                TopRatedScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(Routes.details(itemId))
                    }
                )
            }

            composable(Routes.LOWEST_RATED) {
                LowestRatedScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(Routes.details(itemId))
                    }
                )
            }

            composable(Routes.PROFILE) { backStackEntry ->
                val profileViewModel: ProfileViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    backStackEntry.savedStateHandle
                        .getStateFlow<String?>("selected_item", null)
                        .collect { itemId ->
                            if (itemId != null) {
                                profileViewModel.addFavourite(itemId)
                                backStackEntry.savedStateHandle["selected_item"] = null
                            }
                        }
                }

                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onReviewClick = { reviewId ->
                        navController.navigate(Routes.reviewDetail(reviewId))
                    },
                    onAddFavouriteClick = {
                        navController.navigate(Routes.FAVOURITE_PICKER)
                    },
                    viewModel = profileViewModel
                )
            }

            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSettingsClick = {},
                    onReviewClick = { reviewId ->
                        navController.navigate(Routes.reviewDetail(reviewId))
                    },
                    onAddFavouriteClick = {},
                    viewModel = hiltViewModel()
                )
            }

            composable(Routes.FAVOURITE_PICKER) {
                FavouritePickerScreen(
                    onBack = { navController.popBackStack() },
                    onItemSelected = { itemId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_item", itemId)
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    val current = currentBackStackEntry?.destination?.route
    if (current == route) return
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.openUserProfile(userId: String, currentUserId: String?) {
    if (userId == currentUserId) {
        navigate(Routes.PROFILE) { launchSingleTop = true }
    } else {
        navigate(Routes.userProfile(userId))
    }
}