package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.TextCaseStyle

interface SettingsRepository {
    val textCaseStyle: Flow<TextCaseStyle>
    suspend fun setTextCaseStyle(style: TextCaseStyle)
}