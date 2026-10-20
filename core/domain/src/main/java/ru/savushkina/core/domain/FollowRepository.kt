package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.FollowResult

interface FollowRepository {
    /** На кого подписан пользователь. Эмитит список uid. */
    fun observeFollowing(userId: String): Flow<List<String>>

    /** Кто подписан на пользователя. Эмитит список uid. */
    fun observeFollowers(userId: String): Flow<List<String>>

    /** true, если текущий пользователь подписан на target. */
    fun observeIsFollowing(followerId: String, followingId: String): Flow<Boolean>

    suspend fun follow(followerId: String, followingId: String): Result<FollowResult>
    suspend fun unfollow(followerId: String, followingId: String): Result<FollowResult>

    /** Счётчики для профиля (без подписки на Flow — один раз). */
    suspend fun getFollowCounts(userId: String): Result<Pair<Int, Int>>
}