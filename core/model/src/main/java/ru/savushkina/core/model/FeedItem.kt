package ru.savushkina.core.model

data class FeedItem(
    val id: String = "",
    val ownerUid: String = "",
    val type: FeedItemType = FeedItemType.NEW_FOLLOWER,
    val actorUid: String = "",
    val actorName: String = "",
    val actorAvatarBase64: String = "",
    val reviewId: String = "",
    val reviewComment: String = "",
    val itemName: String = "",
    val rating: Int = 0,
    val commentText: String = "",
    val reaction: Reaction? = null,
    val achievementType: String = "",
    val achievementLevel: Int = 0,
    val timestampMillis: Long = 0L
)

enum class FeedItemType {
    NEW_FOLLOWER,
    REVIEW_REACTION,
    REVIEW_COMMENT,
    FOLLOWING_REVIEW,
    FOLLOWING_COMMENT,
    ACHIEVEMENT_UNLOCKED
}

fun feedItemId(type: FeedItemType, sourceId: String, ownerUid: String): String =
    "${type.name.lowercase()}_${sourceId}_$ownerUid"