package ru.savushkina.core.model

data class Follow(
    val followerId: String = "",
    val followingId: String = "",
    val createdAtMillis: Long = 0L
) {
    val id: String get() = "$followerId|$followingId"
}

sealed interface FollowResult {
    data object Followed : FollowResult
    data object Unfollowed : FollowResult
    data object AlreadyFollowing : FollowResult
    data object NotFollowing : FollowResult
    data object CannotFollowSelf : FollowResult
}