package ru.savushkina.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.FavouriteFourSection
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.KeyValueRow
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.ReviewRow
import ru.savushkina.core.designsystem.component.SectionHeader
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

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewClick: (reviewId: String) -> Unit,
    onAddFavouriteClick: (slotIndex: Int) -> Unit,
    onCollectionsClick: (userId: String?) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        reviews = reviews,
        favourites = favourites,
        isOwnProfile = viewModel.isOwnProfile,
        currentUserId = viewModel.currentUserId,
        onBack = onBack,
        onSettingsClick = onSettingsClick,
        onReviewClick = onReviewClick,
        onAddFavouriteClick = onAddFavouriteClick,
        onRemoveFavourite = viewModel::removeFavourite,
        onCollectionsClick = {
            val uid = (state as? ProfileUiState.Success)?.user?.uid
            onCollectionsClick(if (viewModel.isOwnProfile) null else uid)
        }
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    reviews: List<Review> = emptyList(),
    favourites: List<Item> = emptyList(),
    isOwnProfile: Boolean = true,
    currentUserId: String? = null,
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReviewClick: (reviewId: String) -> Unit = {},
    onAddFavouriteClick: (slotIndex: Int) -> Unit = {},
    onRemoveFavourite: (String) -> Unit = {},
    onCollectionsClick: () -> Unit = {}
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

            when (state) {
                is ProfileUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is ProfileUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileBody(
                        user = state.user,
                        reviews = reviews,
                        favourites = favourites,
                        isOwnProfile = isOwnProfile,
                        currentUserId = currentUserId,
                        onReviewClick = onReviewClick,
                        onAddFavouriteClick = onAddFavouriteClick,
                        onRemoveFavourite = onRemoveFavourite,
                        onCollectionsClick = onCollectionsClick
                    )
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
    onReviewClick: (reviewId: String) -> Unit,
    onAddFavouriteClick: (slotIndex: Int) -> Unit,
    onRemoveFavourite: (String) -> Unit,
    onCollectionsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
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

            Spacer(Modifier.height(Spacing.xl))

            FavouriteFourSection(
                favourites = favourites,
                isOwnProfile = isOwnProfile,
                onSlotClick = onAddFavouriteClick,
                onRemoveFavourite = onRemoveFavourite
            )

            Spacer(Modifier.height(Spacing.md))

            WireButton(
                text = "коллекции",
                onClick = onCollectionsClick
            )

            Spacer(Modifier.height(Spacing.xl))

            SectionHeader(text = if (isOwnProfile) "мои отзывы" else "отзывы")
            Spacer(Modifier.height(Spacing.md))

            if (reviews.isEmpty()) {
                LabelText(text = "// пока нет отзывов")
            } else {
                reviews.forEach { review ->
                    val isMine = currentUserId != null && review.userId == currentUserId
                    ReviewRow(
                        review = review,
                        isMine = isMine,
                        onCardClick = { onReviewClick(review.reviewId) },
                        onCommentsClick = { onReviewClick(review.reviewId) }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        }
    }
}