package ru.savushkina.codeboxd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.feature.collection.ArrayPickerScreen
import com.example.feature.collection.CollectionDetailScreen
import com.example.feature.collection.CollectionPickerScreen
import com.example.feature.collection.CollectionTypePickerScreen
import com.example.feature.collection.CollectionsListScreen
import com.example.feature.collection.CreateCollectionScreen
import com.example.feature.favourite_picker.FavouritePickerScreen
import com.example.feature.search.LowestRatedScreen
import com.example.feature.search.SearchScreen
import com.example.feature.search.TopRatedScreen
import com.example.feature.search.TypeItemsScreen
import com.example.feature.search.TypesScreen
import com.example.feature.search.UserSearchScreen
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.pattern.BottomTab
import ru.savushkina.core.designsystem.pattern.TerminalBottomBar
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.navigation.FollowListMode
import ru.savushkina.core.navigation.Routes
import ru.savushkina.feature.activity.ActivityScreen
import ru.savushkina.feature.auth.AuthScreen
import ru.savushkina.feature.details.DetailsScreen
import ru.savushkina.feature.feed.FeedScreen
import ru.savushkina.feature.onboarding.OnboardingScreen
import ru.savushkina.feature.profile.FollowListScreen
import ru.savushkina.feature.profile.ProfileScreen
import ru.savushkina.feature.profile.ProfileViewModel
import ru.savushkina.feature.review.ReviewDetailScreen
import ru.savushkina.feature.review.ReviewReactionsScreen
import ru.savushkina.feature.review.ReviewScreen
import ru.savushkina.feature.settings.SettingsScreen

@Composable
fun CodeboxNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navTarget by sessionViewModel.navTarget.collectAsStateWithLifecycle()
    val currentUserId by sessionViewModel.currentUserId.collectAsStateWithLifecycle()
    val canRetry by sessionViewModel.canRetry.collectAsStateWithLifecycle()
    val retrying by sessionViewModel.retrying.collectAsStateWithLifecycle()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        ?: Routes.SPLASH

    LaunchedEffect(navTarget) {
        val target = navTarget ?: return@LaunchedEffect
        val current = navController.currentDestination?.route

        when (target) {
            Routes.AUTH -> if (current != Routes.AUTH) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            Routes.ONBOARDING -> if (current != Routes.ONBOARDING) {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            Routes.FEED -> {
                val sessionRoutes = setOf(
                    Routes.SPLASH, Routes.AUTH, Routes.ONBOARDING, Routes.FEED
                )
                if (current in sessionRoutes && current != Routes.FEED) {
                    navController.navigate(Routes.FEED) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val bottomBarRoutes = setOf(
        Routes.FEED, Routes.SEARCH, Routes.ACTIVITY, Routes.PROFILE
    )
    val showBottomBar = currentRoute in bottomBarRoutes
    val selectedTab = when (currentRoute) {
        Routes.FEED -> BottomTab.FEED
        Routes.SEARCH -> BottomTab.SEARCH
        Routes.ACTIVITY -> BottomTab.ACTIVITY
        Routes.PROFILE -> BottomTab.PROFILE
        else -> null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                TerminalBottomBar(
                    selectedTab = selectedTab,
                    onFeedClick = { navController.navigateSingleTop(Routes.FEED) },
                    onSearchClick = { navController.navigateSingleTop(Routes.SEARCH) },
                    onActivityClick = { navController.navigateSingleTop(Routes.ACTIVITY) },
                    onProfileClick = { navController.navigateSingleTop(Routes.PROFILE) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SPLASH) {
                TerminalScreen {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (canRetry) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                AlertBox(text = "Не удалось загрузить профиль")
                                WireButton(
                                    text = if (retrying) "загрузка..." else "повторить",
                                    onClick = sessionViewModel::retry,
                                    accent = true,
                                    enabled = !retrying
                                )
                            }
                        } else {
                            BlinkingCursor()
                        }
                    }
                }
            }

            composable(Routes.AUTH) { AuthScreen() }
            composable(Routes.ONBOARDING) { OnboardingScreen() }

            composable(Routes.FEED) {
                FeedScreen(
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) }
                )
            }

            composable(Routes.ACTIVITY) {
                ActivityScreen(
                    onUserClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    },
                    onReviewClick = { reviewId, commentId ->
                        navController.navigate(Routes.reviewDetail(reviewId, commentId))
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
                    },
                    currentUserId = currentUserId
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
                arguments = listOf(
                    navArgument("reviewId") { type = NavType.StringType },
                    navArgument("commentId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val reviewId = entry.arguments?.getString("reviewId").orEmpty()
                ReviewDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    },
                    onReactionsClick = {
                        navController.navigate(Routes.reviewReactions(reviewId))
                    },
                    currentUserId = currentUserId
                )
            }

            composable(
                route = Routes.REVIEW_REACTIONS,
                arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
            ) {
                ReviewReactionsScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) },
                    onSearchByTypeClick = { navController.navigate(Routes.TYPES) },
                    onTopRatedClick = { navController.navigate(Routes.TOP_RATED) },
                    onLowestRatedClick = { navController.navigate(Routes.LOWEST_RATED) },
                    onUserSearchClick = { navController.navigate(Routes.USER_SEARCH) }
                )
            }

            composable(Routes.USER_SEARCH) {
                UserSearchScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    }
                )
            }

            composable(Routes.TYPES) {
                TypesScreen(
                    onBack = { navController.popBackStack() },
                    onTypeClick = { type -> navController.navigate(Routes.typeItems(type)) }
                )
            }

            composable(
                route = Routes.TYPE_ITEMS,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) {
                TypeItemsScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) }
                )
            }

            composable(Routes.TOP_RATED) {
                TopRatedScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) }
                )
            }

            composable(Routes.LOWEST_RATED) {
                LowestRatedScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) }
                )
            }

            composable(Routes.PROFILE) { backStackEntry ->
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val selectedItemFlow = remember(backStackEntry) {
                    backStackEntry.savedStateHandle
                        .getStateFlow<String?>("selected_item", null)
                }
                LaunchedEffect(selectedItemFlow, profileViewModel) {
                    selectedItemFlow.collect { itemId ->
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
                    onAddFavouriteClick = { navController.navigate(Routes.FAVOURITE_PICKER) },
                    onCollectionsClick = { userId ->
                        navController.navigate(Routes.collections(userId))
                    },
                    onFollowersClick = {
                        navController.navigate(Routes.followList(FollowListMode.FOLLOWERS, null))
                    },
                    onFollowingClick = {
                        navController.navigate(Routes.followList(FollowListMode.FOLLOWING, null))
                    },
                    viewModel = profileViewModel
                )
            }

            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
                val targetUserId = entry.arguments?.getString("userId").orEmpty()
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSettingsClick = {},
                    onReviewClick = { reviewId ->
                        navController.navigate(Routes.reviewDetail(reviewId))
                    },
                    onAddFavouriteClick = {},
                    onCollectionsClick = { userId ->
                        navController.navigate(Routes.collections(userId))
                    },
                    onFollowersClick = {
                        navController.navigate(
                            Routes.followList(FollowListMode.FOLLOWERS, targetUserId)
                        )
                    },
                    onFollowingClick = {
                        navController.navigate(
                            Routes.followList(FollowListMode.FOLLOWING, targetUserId)
                        )
                    },
                    viewModel = hiltViewModel()
                )
            }

            composable(
                route = Routes.FOLLOW_LIST,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("userId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                FollowListScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.openUserProfile(userId, currentUserId)
                    }
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
                            popUpTo(Routes.PROFILE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Routes.COLLECTIONS,
                arguments = listOf(
                    navArgument("userId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                CollectionsListScreen(
                    onBack = { navController.popBackStack() },
                    onCollectionClick = { id ->
                        navController.navigate(Routes.collectionDetail(id))
                    },
                    onCreateClick = { navController.navigate(Routes.COLLECTION_CREATE) }
                )
            }

            composable(Routes.COLLECTION_CREATE) { entry ->
                CreateCollectionScreen(
                    backStackEntry = entry,
                    onBack = { navController.popBackStack() },
                    onListCreated = { id ->
                        navController.navigate(Routes.collectionDetail(id)) {
                            popUpTo(Routes.COLLECTIONS) { inclusive = false }
                        }
                    },
                    onGoToTypePicker = {
                        navController.navigate(Routes.COLLECTION_TYPE_PICKER)
                    },
                    onGoToArrayPicker = { name, type ->
                        navController.navigate(Routes.collectionArrayPicker(name, type))
                    }
                )
            }

            composable(Routes.COLLECTION_TYPE_PICKER) {
                CollectionTypePickerScreen(
                    onBack = { navController.popBackStack() },
                    onTypeSelected = { type ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_type", type)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Routes.COLLECTION_ARRAY_PICKER,
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                ArrayPickerScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.navigate(Routes.collectionDetail(id)) {
                            popUpTo(Routes.COLLECTIONS) { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
            ) {
                CollectionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Routes.details(itemId)) },
                    onAddItemClick = { collectionId ->
                        navController.navigate(Routes.collectionPicker(collectionId))
                    },
                    onDeleted = {
                        navController.popBackStack(Routes.COLLECTIONS, inclusive = false)
                    }
                )
            }

            composable(
                route = Routes.COLLECTION_PICKER,
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
            ) {
                CollectionPickerScreen(
                    onBack = { navController.popBackStack() },
                    onItemAdded = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openUserProfile(userId: String, currentUserId: String?) {
    if (userId == currentUserId) {
        navigateSingleTop(Routes.PROFILE)
    } else {
        navigate(Routes.userProfile(userId))
    }
}