package ru.savushkina.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.model.TextCaseStyle
import javax.inject.Inject

/**
 * Лёгкий ViewModel только для глобальных настроек приложения
 * (например, стиль текста). Используется в MainActivity.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val textCaseStyle: StateFlow<TextCaseStyle> = settingsRepository.textCaseStyle
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TextCaseStyle.NORMAL
        )
}