package ru.savushkina.core.domain

import ru.savushkina.core.model.ItemSuggestion

interface SuggestionRepository {
    /**
     * Отправляет заявку в Firestore-коллекцию `suggestions`.
     * Возвращает id созданного документа.
     */
    suspend fun submitSuggestion(suggestion: ItemSuggestion): Result<String>
}