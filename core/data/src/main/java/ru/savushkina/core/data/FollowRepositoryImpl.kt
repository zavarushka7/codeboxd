package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FeedWriter
import ru.savushkina.core.domain.FollowRepository
import ru.savushkina.core.model.FeedItemType
import ru.savushkina.core.model.FollowResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val feedWriter: FeedWriter
) : FollowRepository {

    override fun observeFollowing(userId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_FOLLOWS)
            .whereEqualTo("followerId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val ids = snapshot?.documents
                    ?.mapNotNull { it.getString("followingId") }
                    .orEmpty()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    override fun observeFollowers(userId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_FOLLOWS)
            .whereEqualTo("followingId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                val ids = snapshot?.documents
                    ?.mapNotNull { it.getString("followerId") }
                    .orEmpty()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    override fun observeIsFollowing(
        followerId: String,
        followingId: String
    ): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection(COLLECTION_FOLLOWS)
            .document(followDocId(followerId, followingId))
            .addSnapshotListener { doc, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (isClosedForSend) return@addSnapshotListener
                trySend(doc?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun follow(
        followerId: String,
        followingId: String
    ): Result<FollowResult> = runCatchingCancellable {
        if (followerId == followingId) {
            return@runCatchingCancellable FollowResult.CannotFollowSelf
        }
        val docRef = firestore.collection(COLLECTION_FOLLOWS)
            .document(followDocId(followerId, followingId))

        val isNew = java.util.concurrent.atomic.AtomicBoolean(false)
        val result: FollowResult = firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (snap.exists()) {
                FollowResult.AlreadyFollowing
            } else {
                isNew.set(true)
                tx.set(
                    docRef,
                    mapOf(
                        "followerId" to followerId,
                        "followingId" to followingId,
                        "createdAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                FollowResult.Followed
            }
        }.await()

        if (isNew.get()) {
            // Профиль подписчика нужен для денормализации в feed.
            val follower = runCatching {
                firestore.collection(COLLECTION_USERS).document(followerId).get().await()
            }.getOrNull()
            feedWriter.fanOut(
                actorUid = followerId,
                sourceId = followDocId(followerId, followingId),
                type = FeedItemType.NEW_FOLLOWER,
                payload = FeedWriter.FeedPayload(
                    actorName = follower?.getString("name").orEmpty(),
                    actorAvatarBase64 = follower?.getString("avatarBase64").orEmpty()
                ),
                extraOwnerUid = followingId
            )
        }
        result
    }

    override suspend fun unfollow(
        followerId: String,
        followingId: String
    ): Result<FollowResult> = runCatchingCancellable {
        val docRef = firestore.collection(COLLECTION_FOLLOWS)
            .document(followDocId(followerId, followingId))
        firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (!snap.exists()) {
                FollowResult.NotFollowing
            } else {
                tx.delete(docRef)
                FollowResult.Unfollowed
            }
        }.await()
    }

    override suspend fun getFollowCounts(userId: String): Result<Pair<Int, Int>> =
        runCatchingCancellable {
            val followersCount = firestore.collection(COLLECTION_FOLLOWS)
                .whereEqualTo("followingId", userId)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            val followingCount = firestore.collection(COLLECTION_FOLLOWS)
                .whereEqualTo("followerId", userId)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            followersCount.toInt() to followingCount.toInt()
        }

    private fun followDocId(followerId: String, followingId: String): String =
        "$followerId|$followingId"

    private companion object {
        const val COLLECTION_FOLLOWS = "follows"
        const val COLLECTION_USERS = "users"
    }
}