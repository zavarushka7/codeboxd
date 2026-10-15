package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconKey(iconKey: String, modifier: Modifier = Modifier) {
    Icon(
        imageVector = iconKey.toIcon(),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(22.dp)
    )
}

private fun String.toIcon(): ImageVector = when (this) {
    "code" -> Icons.Outlined.Code
    "terminal" -> Icons.Outlined.Terminal
    "bug" -> Icons.Outlined.BugReport
    "cloud" -> Icons.Outlined.Cloud
    "storage" -> Icons.Outlined.Storage
    "palette" -> Icons.Outlined.Palette
    "phone" -> Icons.Outlined.PhoneAndroid
    "security" -> Icons.Outlined.Security
    "chart" -> Icons.Outlined.BarChart
    "robot" -> Icons.Outlined.SmartToy
    "game" -> Icons.Outlined.VideogameAsset
    "build" -> Icons.Outlined.Build
    "web" -> Icons.Outlined.Web
    "dataset" -> Icons.Outlined.Dataset
    "settings" -> Icons.Outlined.Settings
    else -> Icons.Outlined.Code
}