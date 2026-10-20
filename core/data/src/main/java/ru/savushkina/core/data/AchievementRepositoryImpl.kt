package ru.savushkina.core.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.AchievementRepository
import ru.savushkina.core.domain.FeedWriter
import ru.savushkina.core.model.Achievement
import ru.savushkina.core.model.AchievementLevels
import ru.savushkina.core.model.AchievementStats
import ru.savushkina.core.model.AchievementType
import ru.savushkina.core.model.FeedItemType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val feedWriter: FeedWriter
) : AchievementRepository {

    override suspend fun computeStats(userId: String): Result<AchievementStats> =
        runCatchingCancellable {
            val reviewsSnap = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val reviews = reviewsSnap.documents.mapNotNull { doc ->
                val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                val comment = doc.getString("comment") ?: ""
                val likes = doc.getLong("likes")?.toInt() ?: 0
                Triple(rating, comment, likes)
            }

            val totalReviews = reviews.size
            val oneStar = reviews.count { it.first == 1 }
            val fiveStar = reviews.count { it.first == 5 }
            val longReviews = reviews.count { it.second.length > LONG_REVIEW_MIN }
            val likesReceived = reviews.sumOf { it.third }

            val reactionsSnap = firestore.collection(COLLECTION_REACTIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "LIKE")
                .get()
                .await()
            val likesGiven = reactionsSnap.size()

            val unusual = if (totalReviews >= 2) {
                val avg = reviews.map { it.first }.average()
                reviews.count { kotlin.math.abs(it.first - avg) >= 2.0 }
            } else 0

            AchievementStats(
                oneStarReviews = oneStar,
                fiveStarReviews = fiveStar,
                totalReviews = totalReviews,
                longReviews = longReviews,
                likesReceived = likesReceived,
                likesGiven = likesGiven,
                unusualReviews = unusual
            )
        }

    override suspend fun computeAchievements(userId: String): Result<List<Achievement>> =
        runCatchingCancellable {
            val stats = computeStats(userId).getOrThrow()
            AchievementType.entries.map { type ->
                val value = stats.valueFor(type)
                val level = AchievementLevels.levelFor(type, value)
                Achievement(
                    type = type,
                    level = level,
                    currentValue = value,
                    nextThreshold = AchievementLevels.nextThreshold(type, level),
                    levelName = AchievementLevels.levelName(type, level),
                    description = AchievementLevels.descriptions[type] ?: "",
                    iconKey = AchievementLevels.icons[type] ?: "code"
                )
            }
        }

    override suspend fun checkAndNotifyLevelUps(userId: String): Result<List<Achievement>> =
        runCatchingCancellable {
            val userRef = firestore.collection(COLLECTION_USERS).document(userId)
            val userSnap = userRef.get().await()
            val storedLevels: Map<String, Long> =
                (userSnap.get("achievementLevels") as? Map<*, *>)
                    ?.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = (v as? Number)?.toLong() ?: return@mapNotNull null
                        key to value
                    }
                    ?.toMap()
                    .orEmpty()

            val stats = computeStats(userId).getOrThrow()
            val actorName = userSnap.getString("name").orEmpty()
            val actorAvatar = userSnap.getString("avatarBase64").orEmpty()

            val levelUps = mutableListOf<Achievement>()
            val levelsToWrite = mutableMapOf<String, Any>()

            AchievementType.entries.forEach { type ->
                val value = stats.valueFor(type)
                val newLevel = AchievementLevels.levelFor(type, value)
                val oldLevel = (storedLevels[type.name] ?: 0L).toInt()
                if (newLevel > oldLevel) {
                    val achievement = Achievement(
                        type = type,
                        level = newLevel,
                        currentValue = value,
                        nextThreshold = AchievementLevels.nextThreshold(type, newLevel),
                        levelName = AchievementLevels.levelName(type, newLevel),
                        description = AchievementLevels.descriptions[type] ?: "",
                        iconKey = AchievementLevels.icons[type] ?: "code"
                    )
                    levelUps += achievement
                    levelsToWrite[type.name] = newLevel

                    feedWriter.fanOut(
                        actorUid = userId,
                        sourceId = "${type.name}_$newLevel",
                        type = FeedItemType.ACHIEVEMENT_UNLOCKED,
                        payload = FeedWriter.FeedPayload(
                            actorName = actorName,
                            actorAvatarBase64 = actorAvatar,
                            achievementType = type.name,
                            achievementLevel = newLevel
                        ),
                        extraOwnerUid = userId
                    )
                }
            }

            if (levelsToWrite.isNotEmpty()) {
                userRef.set(
                    mapOf("achievementLevels" to levelsToWrite),
                    SetOptions.merge()
                ).await()
            }

            levelUps
        }

    private companion object {
        const val COLLECTION_REVIEWS = "reviews"
        const val COLLECTION_REACTIONS = "reactions"
        const val COLLECTION_USERS = "users"
        const val LONG_REVIEW_MIN = 200
    }
}