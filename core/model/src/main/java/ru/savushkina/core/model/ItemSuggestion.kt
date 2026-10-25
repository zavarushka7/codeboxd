package ru.savushkina.core.model

/**
 * Заявка на добавление айтема, которую пользователь отправляет админу.
 *
 * Пишется в Firestore-коллекцию `suggestions`. Админ читает её
 * вручную (консоль Firebase / админ-скрипт) или получает письмо
 * через Firebase Extension «Trigger Email».
 */
data class ItemSuggestion(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val createdAtMillis: Long = 0L,
    val status: SuggestionStatus = SuggestionStatus.PENDING
) {
    companion object {
        const val NAME_MAX = 40
        const val TYPE_MAX = 40
        const val DESCRIPTION_MAX = 500
    }
}

enum class SuggestionStatus {
    PENDING,
    APPROVED,
    REJECTED
}