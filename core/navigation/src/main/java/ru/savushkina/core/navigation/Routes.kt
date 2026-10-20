package ru.savushkina.core.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val FEED = "feed"
    const val SEARCH = "search"
    const val TYPES = "types"
    const val TYPE_ITEMS = "type_items/{type}"
    const val TOP_RATED = "top_rated"
    const val LOWEST_RATED = "lowest_rated"
    const val PROFILE = "profile"
    const val USER_PROFILE = "user/{userId}"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{itemId}"
    const val USER_SEARCH = "user_search"

    fun userSearch() = USER_SEARCH
    const val REVIEW_FORM = "review_form/{itemId}"

    // reviewId опционально + commentId опционально (для скролла к комментарию из Activity)
    const val REVIEW_DETAIL = "review_detail/{reviewId}?commentId={commentId}"

    const val FAVOURITE_PICKER = "favourite_picker"

    const val COLLECTIONS = "collections?userId={userId}"
    const val COLLECTION_CREATE = "collection_create"
    const val COLLECTION_TYPE_PICKER = "collection_type_picker"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val COLLECTION_PICKER = "collection_picker/{collectionId}"
    const val COLLECTION_ARRAY_PICKER = "collection_array_picker?name={name}&type={type}"

    const val ACTIVITY = "activity"
    const val REVIEW_REACTIONS = "review_reactions/{reviewId}"

    // userId опционально — если не передан, показываем свой список.
    const val FOLLOW_LIST = "follow_list/{mode}?userId={userId}"

    // ── path args ──
    fun details(itemId: String) = "details/${Uri.encode(itemId)}"
    fun reviewForm(itemId: String) = "review_form/${Uri.encode(itemId)}"

    fun reviewDetail(reviewId: String, commentId: String? = null): String {
        val encodedReview = Uri.encode(reviewId)
        return if (commentId.isNullOrBlank()) {
            "review_detail/$encodedReview?commentId="
        } else {
            "review_detail/$encodedReview?commentId=${Uri.encode(commentId)}"
        }
    }

    fun reviewReactions(reviewId: String) = "review_reactions/${Uri.encode(reviewId)}"
    fun userProfile(userId: String) = "user/${Uri.encode(userId)}"
    fun typeItems(type: String) = "type_items/${Uri.encode(type)}"
    fun collectionDetail(collectionId: String) = "collection/${Uri.encode(collectionId)}"
    fun collectionPicker(collectionId: String) = "collection_picker/${Uri.encode(collectionId)}"

    // ── query args ──
    fun collections(userId: String? = null): String =
        if (userId.isNullOrBlank()) "collections"
        else "collections?userId=${Uri.encode(userId)}"

    fun collectionArrayPicker(name: String, type: String): String {
        val encodedName = Uri.encode(name)
        val encodedType = Uri.encode(type)
        return "collection_array_picker?name=$encodedName&type=$encodedType"
    }

    fun followList(mode: FollowListMode, userId: String? = null): String {
        val base = "follow_list/${mode.routeValue}"
        return if (userId.isNullOrBlank()) "$base?userId="
        else "$base?userId=${Uri.encode(userId)}"
    }

    // ── декодер ──
    fun decode(raw: String?): String = raw?.let(Uri::decode).orEmpty()
}

enum class FollowListMode(val routeValue: String) {
    FOLLOWERS("followers"),
    FOLLOWING("following");

    companion object {
        fun fromRoute(value: String?): FollowListMode =
            entries.firstOrNull { it.routeValue == value } ?: FOLLOWERS
    }
}