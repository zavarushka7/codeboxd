package com.example.feature.collection

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.collectLatest
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.KeyValueRow
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.CollectionKind

@Composable
fun CreateCollectionScreen(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onListCreated: (String) -> Unit,
    onGoToTypePicker: () -> Unit,
    onGoToArrayPicker: (name: String, type: String) -> Unit,
    viewModel: CreateCollectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val selectedTypeFlow = remember(backStackEntry) {
        backStackEntry.savedStateHandle
            .getStateFlow<String?>("selected_type", null)
    }

    LaunchedEffect(selectedTypeFlow) {
        selectedTypeFlow.collectLatest { type ->
            if (type != null) {
                viewModel.onAllowedTypeChange(type)
                backStackEntry.savedStateHandle["selected_type"] = null
            }
        }
    }

    CreateCollectionContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onKindChange = viewModel::onKindChange,
        onPickTypeClick = onGoToTypePicker,
        onCreateList = { viewModel.createList(onListCreated) },
        onNextForArray = {
            if (viewModel.validateForArray()) {
                onGoToArrayPicker(
                    state.name.trim(),
                    state.allowedType ?: ""
                )
            }
        }
    )
}

@Composable
fun CreateCollectionContent(
    state: CreateCollectionState,
    onBack: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onKindChange: (CollectionKind) -> Unit = {},
    onPickTypeClick: () -> Unit = {},
    onCreateList: () -> Unit = {},
    onNextForArray: () -> Unit = {}
) {
    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// новая коллекция",
                left = {
                    LabelText(
                        text = "< назад",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onBack)
                    )
                }
            )
            HorizontalLine()
            Spacer(Modifier.height(Spacing.md))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                WireTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "имя",
                    placeholder = "например: мои языки",
                    enabled = !state.isLoading
                )

                Spacer(Modifier.height(Spacing.xl))

                LabelText(text = "// тип", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))

                ChoiceRow(
                    text = "список — пустой, можно редактировать",
                    selected = state.kind == CollectionKind.LIST,
                    onClick = { onKindChange(CollectionKind.LIST) }
                )
                Spacer(Modifier.height(Spacing.xs))
                ChoiceRow(
                    text = "массив — заполняется сразу, потом нельзя менять",
                    selected = state.kind == CollectionKind.ARRAY,
                    onClick = { onKindChange(CollectionKind.ARRAY) }
                )

                if (state.kind == CollectionKind.ARRAY) {
                    Spacer(Modifier.height(Spacing.xl))

                    LabelText(
                        text = "// тип айтемов",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    WireFrame(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onPickTypeClick)
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LabelText(
                                text = state.allowedType ?: "выбрать тип",
                                color = if (state.allowedType != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LabelText(text = ">", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(Spacing.md))
                    AlertBox(text = it)
                }

                Spacer(Modifier.height(Spacing.xl))

                if (state.kind == CollectionKind.LIST) {
                    WireButton(
                        text = if (state.isLoading) "создание..." else "создать",
                        onClick = onCreateList,
                        accent = true,
                        enabled = !state.isLoading
                    )
                } else {
                    WireButton(
                        text = "далее →",
                        onClick = onNextForArray,
                        accent = true,
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    KeyValueRow(
        key = if (selected) "[x]" else "[ ]",
        value = text,
        accent = selected,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs)
    )
}