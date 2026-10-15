package ru.savushkina.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.model.TextCaseStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val textCaseStyle: Flow<TextCaseStyle> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_TEXT_CASE] ?: TextCaseStyle.NORMAL.name
        runCatching { TextCaseStyle.valueOf(raw) }.getOrDefault(TextCaseStyle.NORMAL)
    }

    override suspend fun setTextCaseStyle(style: TextCaseStyle) {
        dataStore.edit { prefs ->
            prefs[KEY_TEXT_CASE] = style.name
        }
    }

    private companion object {
        val KEY_TEXT_CASE = stringPreferencesKey("text_case_style")
    }
}