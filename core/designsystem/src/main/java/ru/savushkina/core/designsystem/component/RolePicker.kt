package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role

@Composable
fun RolePicker(
    selectedRole: Role?,
    onRoleSelect: (Role) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Role.entries.forEach { role ->
            val selected = selectedRole == role
            KeyValueRow(
                key = if (selected) "[x]" else "[ ]",
                value = role.displayName,
                accent = selected,
                modifier = Modifier
                    .clickable(enabled = enabled) { onRoleSelect(role) }
                    .padding(vertical = Spacing.xs)
            )
        }
    }
}