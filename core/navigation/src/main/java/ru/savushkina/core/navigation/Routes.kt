package ru.savushkina.core.navigation

object Routes {
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

    /** Коллекции. Если userId = null — свои. */
    const val COLLECTIONS = "collections?userId={userId}"
    const val COLLECTION_CREATE = "collection_create"
    const val COLLECTION_TYPE_PICKER = "collection_type_picker"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val COLLECTION_PICKER = "collection_picker/{collectionId}"
    const val COLLECTION_ARRAY_PICKER = "collection_array_picker?name={name}&type={type}"

    fun details(itemId: String) = "details/$itemId"
    fun reviewForm(itemId: String) = "review_form/$itemId"
    fun reviewDetail(reviewId: String) = "review_detail/$reviewId"
    fun userProfile(userId: String) = "user/$userId"
    fun typeItems(type: String) = "type_items/${java.net.URLEncoder.encode(type, "UTF-8")}"

    fun collections(userId: String? = null): String =
        if (userId.isNullOrBlank()) "collections"
        else "collections?userId=$userId"

    fun collectionDetail(collectionId: String) = "collection/$collectionId"
    fun collectionPicker(collectionId: String) = "collection_picker/$collectionId"

    fun collectionArrayPicker(name: String, type: String): String {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val encodedType = java.net.URLEncoder.encode(type, "UTF-8")
        return "collection_array_picker?name=$encodedName&type=$encodedType"
    }
}