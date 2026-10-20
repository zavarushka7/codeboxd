package ru.savushkina.core.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FeedWriter
import ru.savushkina.core.model.FeedItemType
import ru.savushkina.core.model.feedItemId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedWriterImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FeedWriter {

    override suspend fun fanOut(
        actorUid: String,
        sourceId: String,
        type: FeedItemType,
        payload: FeedWriter.FeedPayload,
        extraOwnerUid: String?
    ): Result<Unit> = runCatchingCancellable {
        val now = System.currentTimeMillis()

        val recipients = buildSet {
            val followers = firestore.collection(COLLECTION_FOLLOWS)
                .whereEqualTo("followingId", actorUid)
                .limit(FeedWriter.MAX_FANOUT.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("followerId") }
            addAll(followers)
            if (!extraOwnerUid.isNullOrBlank()) add(extraOwnerUid)
            remove(actorUid)
            // Для ACHIEVEMENT_UNLOCKED пишем и себе — чтобы уведомление
            // было в собственной ленте.
            if (type == FeedItemType.ACHIEVEMENT_UNLOCKED) add(actorUid)
        }

        if (recipients.isEmpty()) return@runCatchingCancellable

        if (recipients.size >= FeedWriter.MAX_FANOUT) {
            Log.w(
                TAG,
                "fanOut: recipients=${recipients.size} hit MAX_FANOUT"
            )
        }

        recipients.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ownerUid ->
                val docId = feedItemId(type, sourceId, ownerUid)
                val ref = firestore.collection(COLLECTION_FEEDS)
                    .document(ownerUid)
                    .collection(COLLECTION_ITEMS)
                    .document(docId)
                val data = mapOf(
                    "ownerUid" to ownerUid,
                    "type" to type.name,
                    "actorUid" to actorUid,
                    "actorName" to payload.actorName,
                    "actorAvatarBase64" to payload.actorAvatarBase64,
                    "reviewId" to payload.reviewId,
                    "reviewComment" to payload.reviewComment,
                    "itemName" to payload.itemName,
                    "rating" to payload.rating,
                    "commentText" to payload.commentText,
                    "reaction" to payload.reaction?.name,
                    "achievementType" to payload.achievementType,
                    "achievementLevel" to payload.achievementLevel,
                    "timestampMillis" to now
                )
                batch.set(ref, data, SetOptions.merge())
            }
            batch.commit().await()
        }
        Unit
    }

    private companion object {
        const val TAG = "FeedWriterImpl"
        const val COLLECTION_FEEDS = "feeds"
        const val COLLECTION_ITEMS = "items"
        const val COLLECTION_FOLLOWS = "follows"
        const val BATCH_LIMIT = 500
    }
}