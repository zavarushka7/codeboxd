package ru.savushkina.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.*
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Role
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

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAvatarSelected(it, context) }
    }

    SettingsContent(
        state = state,
        draft = draft,
        isSaving = isSaving,
        error = error,
        onBack = onBack,
        onPickAvatar = { imagePicker.launch("image/*") },
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onRoleChange = viewModel::onRoleChange,
        onSave = { viewModel.save(onSuccess = onSaved) },
        onSignOut = viewModel::signOut
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    draft: User? = null,
    isSaving: Boolean = false,
    error: String? = null,
    onBack: () -> Unit = {},
    onPickAvatar: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onRoleChange: (Role) -> Unit = {},
    onSave: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    TerminalScreen(topPadding = Spacing.xxl) {
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
                        onPickAvatar = onPickAvatar,
                        onNameChange = onNameChange,
                        onDescriptionChange = onDescriptionChange,
                        onRoleChange = onRoleChange,
                        onSave = onSave,
                        onSignOut = onSignOut
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
    onPickAvatar: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRoleChange: (Role) -> Unit,
    onSave: () -> Unit,
    onSignOut: () -> Unit = {}
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
                value = user.description,
                onValueChange = onDescriptionChange,
                label = "about",
                placeholder = "пара_слов_о_себе",
                singleLine = false,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.xl))

            // ── Выбор роли ──
            LabelText(
                text = "// ваша роль",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))

            Role.entries.forEach { role ->
                val selected = user.role == role
                KeyValueRow(
                    key = if (selected) "[x]" else "[ ]",
                    value = role.displayName,
                    accent = selected,
                    modifier = Modifier
                        .clickable(
                            enabled = !isSaving,
                            onClick = { onRoleChange(role) }
                        )
                        .padding(vertical = Spacing.xs)
                )
            }

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
            WireButton(text = "выйти из аккаунта", onClick = onSignOut)
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