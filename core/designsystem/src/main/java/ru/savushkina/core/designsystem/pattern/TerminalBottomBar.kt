package ru.savushkina.core.designsystem.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.theme.Sizes

enum class BottomTab(val route: String) {
    FEED("feed"),
    SEARCH("search"),
    PROFILE("profile")
}

@Composable
fun TerminalBottomBar(
    selectedTab: BottomTab?,
    onFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.systemBarsPadding()) {
        HorizontalLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.bottomBarHeight)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(Icons.Outlined.Home, "feed",
                selectedTab == BottomTab.FEED, onFeedClick)
            BottomBarItem(Icons.Outlined.Search, "search",
                selectedTab == BottomTab.SEARCH, onSearchClick)
            BottomBarItem(Icons.Outlined.Person, "profile",
                selectedTab == BottomTab.PROFILE, onProfileClick)
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconMd)
        )
    }
}