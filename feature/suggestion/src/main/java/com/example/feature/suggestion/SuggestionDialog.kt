package ru.savushkina.feature.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.component.WireTextField
import ru.savushkina.core.designsystem.theme.Spacing

/**
 * Диалог «предложить айтем».
 *
 * onDismiss вызывается и по кнопке «отмена», и после успешной отправки.
 * onSuccess опционален — если хочешь показать снек «заявка отправлена»
 * на экране-хозяине.
 */
@Composable
fun SuggestionDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
    viewModel: SuggestionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            viewModel.onDoneHandled()
            onSuccess()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .imePadding()
        ) {
            WireFrame(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(Spacing.md)
                ) {
                    LabelText(text = "// предложить айтем")
                    Spacer(Modifier.height(Spacing.md))

                    if (state.isSending) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            BlinkingCursor()
                        }
                        return@Column
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        WireTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            label = "имя",
                            placeholder = "например: Kotlin"
                        )
                        Spacer(Modifier.height(Spacing.md))

                        WireTextField(
                            value = state.type,
                            onValueChange = viewModel::onTypeChange,
                            label = "тип",
                            placeholder = "например: Язык программирования"
                        )
                        Spacer(Modifier.height(Spacing.md))

                        WireTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = "описание",
                            placeholder = "пара слов о айтеме",
                            singleLine = false,
                            minHeight = 140.dp
                        )

                        state.error?.let {
                            Spacer(Modifier.height(Spacing.md))
                            AlertBox(text = it)
                        }

                        Spacer(Modifier.height(Spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                WireButton(
                                    text = "отмена",
                                    onClick = onDismiss
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                WireButton(
                                    text = "отправить",
                                    onClick = viewModel::submit,
                                    accent = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}