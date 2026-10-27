package ru.savushkina.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.FavouriteFourSection
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.KeyValueRow
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.ReviewRow
import ru.savushkina.core.designsystem.component.SectionHeader
import ru.savushkina.core.designsystem.component.SkeletonProfileHeader
import ru.savushkina.core.designsystem.component.SkeletonReviewRow
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Item
import ru.savushkina.core.model.Review
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.User
import ru.savushkina.core.navigation.FollowListMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    onAddFavouriteClick: (slotIndex: Int) -> Unit,
    onCollectionsClick: (userId: String?) -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val isOwnProfile by viewModel.isOwnProfile.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val followersCount by viewModel.followersCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val followActionLoading by viewModel.followActionLoading.collectAsStateWithLifecycle()

    // ВАЖНО: uid для навигации берём из route (viewModel.targetUserId),
    // а не из state.user.uid. state может быть Loading (uid ещё не пришёл),
    // а навигация должна вести на правильный профиль. И getUser(targetUserId)
    // может вернуть документ с другим uid (кэш/ошибка) — тогда бы мы ушли
    // не в тот профиль.
    val collectionsOwnerUid: String? = if (isOwnProfile) null else viewModel.targetUserId

    ProfileContent(
        state = state,
        reviews = reviews,
        favourites = favourites,
        isOwnProfile = isOwnProfile,
        currentUserId = currentUserId,
        error = error,
        isRefreshing = isRefreshing,
        followersCount = followersCount,
        followingCount = followingCount,
        isFollowing = isFollowing,
        followActionLoading = followActionLoading,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onSettingsClick = onSettingsClick,
        onReviewClick = onReviewClick,
        onAddFavouriteClick = onAddFavouriteClick,
        onRemoveFavourite = viewModel::removeFavourite,
        onCollectionsClick = { onCollectionsClick(collectionsOwnerUid) },
        onFollowersClick = onFollowersClick,
        onFollowingClick = onFollowingClick,
        onToggleFollow = viewModel::toggleFollow,
        onAchievementsClick = onAchievementsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    reviews: List<Review> = emptyList(),
    favourites: List<Item> = emptyList(),
    isOwnProfile: Boolean = true,
    currentUserId: String? = null,
    error: String? = null,
    isRefreshing: Boolean = false,
    followersCount: Int = 0,
    followingCount: Int = 0,
    isFollowing: Boolean = false,
    followActionLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReviewClick: (reviewId: String) -> Unit = {},
    onAddFavouriteClick: (slotIndex: Int) -> Unit = {},
    onRemoveFavourite: (String) -> Unit = {},
    onCollectionsClick: () -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onToggleFollow: () -> Unit = {},
    onAchievementsClick: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = if (isOwnProfile) "// профиль" else "// пользователь",
                left = if (!isOwnProfile) {
                    {
                        LabelText(
                            text = "< назад",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onBack)
                        )
                    }
                } else null,
                right = if (isOwnProfile) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(Sizes.iconMd)
                                .clickable(onClick = onSettingsClick)
                        )
                    }
                } else null
            )
            HorizontalLine()
            Spacer(Modifier.height(Spacing.lg))

            if (error != null) {
                AlertBox(text = error)
                Spacer(Modifier.height(Spacing.md))
            }

            when (state) {
                is ProfileUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        SkeletonProfileHeader()
                        repeat(3) {
                            SkeletonReviewRow()
                        }
                    }
                }
                is ProfileUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is ProfileUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProfileBody(
                            user = state.user,
                            reviews = reviews,
                            favourites = favourites,
                            isOwnProfile = isOwnProfile,
                            currentUserId = currentUserId,
                            followersCount = followersCount,
                            followingCount = followingCount,
                            isFollowing = isFollowing,
                            followActionLoading = followActionLoading,
                            onReviewClick = onReviewClick,
                            onAddFavouriteClick = onAddFavouriteClick,
                            onRemoveFavourite = onRemoveFavourite,
                            onCollectionsClick = onCollectionsClick,
                            onFollowersClick = onFollowersClick,
                            onFollowingClick = onFollowingClick,
                            onToggleFollow = onToggleFollow,
                            onAchievementsClick = onAchievementsClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBody(
    user: User,
    reviews: List<Review>,
    favourites: List<Item>,
    isOwnProfile: Boolean,
    currentUserId: String?,
    followersCount: Int,
    followingCount: Int,
    isFollowing: Boolean,
    followActionLoading: Boolean,
    onReviewClick: (reviewId: String) -> Unit,
    onAddFavouriteClick: (slotIndex: Int) -> Unit,
    onRemoveFavourite: (String) -> Unit,
    onCollectionsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onAchievementsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "header") {
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
                if (user.handle.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    LabelText(
                        text = user.handle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

                Spacer(Modifier.height(Spacing.md))
                FollowStatsRow(
                    isOwnProfile = isOwnProfile,
                    followersCount = followersCount,
                    followingCount = followingCount,
                    onFollowersClick = onFollowersClick,
                    onFollowingClick = onFollowingClick
                )

                if (!isOwnProfile && currentUserId != null) {
                    Spacer(Modifier.height(Spacing.lg))
                    WireButton(
                        text = when {
                            followActionLoading -> "..."
                            isFollowing -> "отписаться"
                            else -> "подписаться"
                        },
                        onClick = onToggleFollow,
                        accent = !isFollowing,
                        enabled = !followActionLoading
                    )
                }
            }
        }

        item(key = "favourites") {
            Column(modifier = Modifier.fillMaxWidth()) {
                FavouriteFourSection(
                    favourites = favourites,
                    isOwnProfile = isOwnProfile,
                    onSlotClick = onAddFavouriteClick,
                    onRemoveFavourite = onRemoveFavourite
                )
                Spacer(Modifier.height(Spacing.md))
                WireButton(text = "коллекции", onClick = onCollectionsClick)
                Spacer(Modifier.height(Spacing.sm))
                WireButton(text = "награды", onClick = onAchievementsClick)
            }
        }

        item(key = "reviews_header") {
            Spacer(Modifier.height(Spacing.xl))
            SectionHeader(text = if (isOwnProfile) "мои отзывы" else "отзывы")
        }

        if (reviews.isEmpty()) {
            item(key = "reviews_empty") {
                LabelText(text = "// пока нет отзывов")
            }
        } else {
            items(reviews, key = { it.reviewId }) { review ->
                val isMine = currentUserId != null && review.userId == currentUserId
                ReviewRow(
                    review = review,
                    isMine = isMine,
                    onCardClick = { onReviewClick(review.reviewId) },
                    onCommentsClick = { onReviewClick(review.reviewId) }
                )
            }
        }
    }
}

@Composable
private fun FollowStatsRow(
    isOwnProfile: Boolean,
    followersCount: Int,
    followingCount: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    WireFrame(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FollowStatItem(
                value = followersCount.toString(),
                label = "подписчики",
                clickable = true,
                onClick = onFollowersClick
            )
            FollowStatItem(
                value = followingCount.toString(),
                label = "подписки",
                clickable = true,
                onClick = onFollowingClick
            )
        }
    }
}

@Composable
private fun FollowStatItem(
    value: String,
    label: String,
    clickable: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (clickable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Spacing.xxs))
        LabelText(
            text = label,
            color = if (clickable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun FollowListMode.routeTitle(isOwn: Boolean): String = when (this) {
    FollowListMode.FOLLOWERS -> "// подписчики"
    FollowListMode.FOLLOWING -> "// подписки"
}