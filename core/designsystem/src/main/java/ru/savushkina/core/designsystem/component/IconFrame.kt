package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Sizes


@Composable
fun IconFrame(
    icon: ImageVector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }

    WireFrame(
        modifier = modifier.size(Sizes.iconFrame),
        accent = accent && enabled,
        cornerBrackets = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun IconFramePreview() {
    CodeboxTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconFrame(icon = Icons.Outlined.Home)
            IconFrame(icon = Icons.Outlined.Search, accent = true)
            IconFrame(icon = Icons.Outlined.Person, enabled = false)
        }
    }
}