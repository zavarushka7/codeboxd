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
    const val REVIEW_FORM = "review_form/{itemId}"
    const val REVIEW_DETAIL = "review_detail/{reviewId}"
    const val FAVOURITE_PICKER = "favourite_picker"

    const val COLLECTIONS = "collections?userId={userId}"
    const val COLLECTION_CREATE = "collection_create"
    const val COLLECTION_TYPE_PICKER = "collection_type_picker"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val COLLECTION_PICKER = "collection_picker/{collectionId}"
    const val COLLECTION_ARRAY_PICKER = "collection_array_picker?name={name}&type={type}"

    // ── path args ──
    fun details(itemId: String) = "details/${Uri.encode(itemId)}"
    fun reviewForm(itemId: String) = "review_form/${Uri.encode(itemId)}"
    fun reviewDetail(reviewId: String) = "review_detail/${Uri.encode(reviewId)}"
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

    // ── декодер (используется во ViewModel'ях) ──
    fun decode(raw: String?): String = raw?.let(Uri::decode).orEmpty()
}