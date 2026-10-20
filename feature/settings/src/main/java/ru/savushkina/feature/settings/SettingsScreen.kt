package ru.savushkina.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.RolePicker
import ru.savushkina.core.designsystem.component.WireAvatar
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role
import ru.savushkina.core.model.TextCaseStyle
import ru.savushkina.core.model.User

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val textCaseStyle by viewModel.textCaseStyle.collectAsStateWithLifecycle()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsStateWithLifecycle()
    val usernameAvailable by viewModel.usernameAvailable.collectAsStateWithLifecycle()

    var confirmSignOut by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAvatarSelected(it) }
    }

    SettingsContent(
        state = state,
        draft = draft,
        isSaving = isSaving,
        error = error,
        isCheckingUsername = isCheckingUsername,
        usernameAvailable = usernameAvailable,
        onBack = onBack,
        onPickAvatar = { imagePicker.launch("image/*") },
        onNameChange = viewModel::onNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onRoleChange = viewModel::onRoleChange,
        onSave = { viewModel.save(onSuccess = onSaved) },
        onSignOut = { confirmSignOut = true },
        textCaseStyle = textCaseStyle,
        onTextCaseStyleChange = viewModel::setTextCaseStyle
    )

    if (confirmSignOut) {
        SignOutDialog(
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false }
        )
    }
}

@Composable
private fun SignOutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        WireFrame(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(Spacing.md)
            ) {
                LabelText(text = "// выйти из аккаунта?")
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WireButton(
                            text = "отмена",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WireButton(
                            text = "выйти",
                            onClick = onConfirm,
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    draft: User? = null,
    isSaving: Boolean = false,
    error: String? = null,
    isCheckingUsername: Boolean = false,
    usernameAvailable: Boolean? = null,
    onBack: () -> Unit = {},
    onPickAvatar: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onRoleChange: (Role) -> Unit = {},
    onSave: () -> Unit = {},
    onSignOut: () -> Unit = {},
    textCaseStyle: TextCaseStyle = TextCaseStyle.NORMAL,
    onTextCaseStyleChange: (TextCaseStyle) -> Unit = {},
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            val canSave = !isSaving && state is SettingsUiState.Success

            TerminalTopBar(
                title = "// настройки",
                left = {
                    LabelText(
                        text = "< назад",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onBack)
                    )
                },
                right = {
                    LabelText(
                        text = if (isSaving) "..." else "сохранить",
                        color = if (canSave) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.clickable(enabled = canSave, onClick = onSave)
                    )
                }
            )
            HorizontalLine()
            Spacer(Modifier.height(Spacing.lg))

            when (state) {
                is SettingsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingCursor()
                    }
                }
                is SettingsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AlertBox(text = state.message)
                    }
                }
                is SettingsUiState.Success -> {
                    val user = draft ?: state.user
                    SettingsForm(
                        user = user,
                        isSaving = isSaving,
                        error = error,
                        isCheckingUsername = isCheckingUsername,
                        usernameAvailable = usernameAvailable,
                        onPickAvatar = onPickAvatar,
                        onNameChange = onNameChange,
                        onUsernameChange = onUsernameChange,
                        onDescriptionChange = onDescriptionChange,
                        onRoleChange = onRoleChange,
                        onSave = onSave,
                        onSignOut = onSignOut,
                        onTextCaseStyleChange = onTextCaseStyleChange,
                        textCaseStyle = textCaseStyle
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsForm(
    user: User,
    isSaving: Boolean,
    error: String?,
    isCheckingUsername: Boolean,
    usernameAvailable: Boolean?,
    onPickAvatar: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRoleChange: (Role) -> Unit,
    onSave: () -> Unit,
    onSignOut: () -> Unit = {},
    textCaseStyle: TextCaseStyle = TextCaseStyle.NORMAL,
    onTextCaseStyleChange: (TextCaseStyle) -> Unit = {},
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
            Spacer(Modifier.height(Spacing.sm))
            LabelText(
                text = "[ сменить аватар ]",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = !isSaving, onClick = onPickAvatar)
            )

            Spacer(Modifier.height(Spacing.xl))

            WireTextField(
                value = user.name,
                onValueChange = onNameChange,
                label = "name",
                placeholder = "ваше_имя",
                enabled = !isSaving
            )
            Spacer(Modifier.height(Spacing.md))

            WireTextField(
                value = user.username,
                onValueChange = onUsernameChange,
                label = "username",
                placeholder = "handle",
                enabled = !isSaving
            )
            Spacer(Modifier.height(Spacing.xs))
            LabelText(
                text = when {
                    isCheckingUsername -> "// проверяем..."
                    usernameAvailable == true -> "// @${user.username} свободен"
                    usernameAvailable == false -> "// @${user.username} занят"
                    user.username.isBlank() -> "// 3–20 символов, a-z, 0-9, _"
                    else -> "// @${user.username}"
                },
                color = when {
                    usernameAvailable == true -> MaterialTheme.colorScheme.primary
                    usernameAvailable == false -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(Spacing.md))

            WireTextField(
                value = user.description,
                onValueChange = onDescriptionChange,
                label = "about",
                placeholder = "пара_слов_о_себе",
                singleLine = false,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.xl))

            LabelText(
                text = "// ваша роль",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))

            RolePicker(
                selectedRole = user.role,
                onRoleSelect = onRoleChange,
                enabled = !isSaving
            )

            error?.let {
                Spacer(Modifier.height(Spacing.md))
                AlertBox(text = it)
            }

            Spacer(Modifier.height(Spacing.xl))
            WireButton(
                text = if (isSaving) "сохранение..." else "сохранить",
                onClick = onSave,
                accent = true,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.xl))

            LabelText(
                text = "// стиль текста",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.sm))

            TextCaseStylePicker(
                selected = textCaseStyle,
                onSelect = onTextCaseStyleChange,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.xl))
            WireButton(text = "выйти из аккаунта", onClick = onSignOut)
        }
    }
}

@Composable
private fun TextCaseStylePicker(
    selected: TextCaseStyle,
    onSelect: (TextCaseStyle) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextCaseStyle.entries.forEach { style ->
            val isSelected = selected == style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelect(style) }
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelText(
                    text = if (isSelected) "[x]" else "[ ]",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsPreview() {
    val user = User(
        uid = "u1",
        email = "alena@codebox.app",
        name = "Алена",
        username = "alena",
        description = "Пишу на Kotlin.",
        role = Role.DEVELOPMENT
    )
    CodeboxTheme {
        SettingsContent(
            state = SettingsUiState.Success(user),
            draft = user
        )
    }
}