package ru.savushkina.core.domain

import ru.savushkina.core.model.Achievement
import ru.savushkina.core.model.AchievementStats

interface AchievementRepository {
    suspend fun computeStats(userId: String): Result<AchievementStats>
    suspend fun computeAchievements(userId: String): Result<List<Achievement>>

    /**
     * Пересчитывает уровни, сравнивает с users/{uid}.achievementLevels,
     * при росте пишет ACHIEVEMENT_UNLOCKED в feeds/{uid}/items
     * (и подписчикам), обновляет achievementLevels.
     * Идемпотентно по sourceId = "TYPE_level".
     */
    suspend fun checkAndNotifyLevelUps(userId: String): Result<List<Achievement>>
}