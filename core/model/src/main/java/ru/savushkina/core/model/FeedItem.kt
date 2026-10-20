package ru.savushkina.core.model

/**
 * Элемент ленты активности. Хранится в `feeds/{ownerUid}/items/{feedItemId}`.
 *
 * Денормализован: все поля для отображения (имя, аватар, текст) копируются
 * в момент события, чтобы чтение ленты было одним запросом без join'ов.
 */
data class FeedItem(
    val id: String = "",
    /** Владелец ленты (кому показываем). */
    val ownerUid: String = "",
    /** Тип события. */
    val type: FeedItemType = FeedItemType.NEW_FOLLOWER,
    /** Кто инициатор (подписчик, лайкнувший, комментатор, автор отзыва). */
    val actorUid: String = "",
    val actorName: String = "",
    val actorAvatarBase64: String = "",
    /** Ссылка на отзыв (для реакций/комментариев/отзывов подписок). */
    val reviewId: String = "",
    /** Текст отзыва (денормализация). */
    val reviewComment: String = "",
    /** Название item'а (денормализация). */
    val itemName: String = "",
    /** Рейтинг отзыва (для FollowingReview). */
    val rating: Int = 0,
    /** Текст комментария (для ReviewComment / FollowingComment). */
    val commentText: String = "",
    /** LIKE / DISLIKE (для ReviewReaction). */
    val reaction: Reaction? = null,
    /** Время события (millis since epoch). */
    val timestampMillis: Long = 0L
)

enum class FeedItemType {
    NEW_FOLLOWER,
    REVIEW_REACTION,
    REVIEW_COMMENT,
    FOLLOWING_REVIEW,
    FOLLOWING_COMMENT
}

/**
 * Детерминированный ID элемента ленты — защита от дублей при повторной записи.
 */
fun feedItemId(type: FeedItemType, sourceId: String, ownerUid: String): String =
    "${type.name.lowercase()}_${sourceId}_$ownerUid"