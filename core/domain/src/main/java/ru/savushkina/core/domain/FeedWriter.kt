package ru.savushkina.core.domain

import ru.savushkina.core.model.FeedItemType

interface FeedWriter {
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
        val reaction: ru.savushkina.core.model.Reaction? = null,
        val achievementType: String = "",
        val achievementLevel: Int = 0
    )

    companion object {
        const val MAX_FANOUT = 400
    }
}