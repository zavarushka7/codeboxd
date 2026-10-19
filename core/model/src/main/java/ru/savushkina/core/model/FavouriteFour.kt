package ru.savushkina.core.model

data class FavouriteFour(
    val userId: String = "",
    val itemIds: List<String> = emptyList()
) {
    companion object {
        const val MAX_SIZE = 4
    }
}

sealed interface AddFavouriteResult {
    data object Added : AddFavouriteResult
    data object AlreadyExists : AddFavouriteResult
    data object LimitReached : AddFavouriteResult
}