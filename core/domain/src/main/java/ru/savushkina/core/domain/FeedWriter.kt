package ru.savushkina.core.domain

import ru.savushkina.core.model.FeedItemType

/**
 * Порт для записи событий в ленты подписчиков.
 *
 * Вызывается из репозиториев при создании отзыва / комментария / реакции /
 * подписки. Реализация пишет батчем в `feeds/{ownerUid}/items`.
 *
 * ВАЖНО: в production для аккаунтов с >500 подписчиков fan-out должен идти
 * через Cloud Function (Firestore Triggers), а не из клиента — иначе
 * клиент упрётся в лимит 500 операций на батч и в правила безопасности.
 * Здесь клиентский fan-out ограничен MAX_FANOUT.
 */
interface FeedWriter {
    /**
     * Записать событие в ленты всех подписчиков [actorUid] (кроме самого
     * [actorUid]) и опционально в ленту [extraOwnerUid] (например, владельцу
     * отзыва при реакции/комментарии).
     *
     * @param sourceId уникальный id источника (reviewId / commentId / followId)
     * @param type тип события
     * @param payload денормализованные поля для отображения
     * @param extraOwnerUid дополнительный получатель (владелец отзыва)
     */
    suspend fun fanOut(
        actorUid: String,
        sourceId: String,
        type: FeedItemType,
        payload: FeedPayload,
        extraOwnerUid: String? = null
    ): Result<Unit>

    data class FeedPayload(
        val actorName: String,
        val actorAvatarBase64: String,
        val reviewId: String = "",
        val reviewComment: String = "",
        val itemName: String = "",
        val rating: Int = 0,
        val commentText: String = "",
        val reaction: ru.savushkina.core.model.Reaction? = null
    )

    companion object {
        /** Максимум подписчиков для клиентского fan-out (лимит батча Firestore = 500). */
        const val MAX_FANOUT = 400
    }
}