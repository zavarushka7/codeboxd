package ru.savushkina.core.designsystem.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.theme.Sizes

@Composable
fun TerminalBottomBar(
    currentRoute: String,
    onFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column {
        HorizontalLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = Icons.Outlined.Home,
                label = "feed",
                selected = currentRoute == "feed",
                onClick = onFeedClick
            )
            BottomBarItem(
                icon = Icons.Outlined.Search,
                label = "search",
                selected = currentRoute == "search",
                onClick = onSearchClick
            )
            BottomBarItem(
                icon = Icons.Outlined.Person,
                label = "profile",
                selected = currentRoute == "profile",
                onClick = onProfileClick
            )
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
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(Sizes.iconMd)
            .clickable(onClick = onClick)
    )
}