package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.AddFavouriteResult

interface FavouriteRepository {
    fun observeFavourites(userId: String): Flow<List<String>>
    suspend fun addFavourite(userId: String, itemId: String): Result<AddFavouriteResult>
    suspend fun removeFavourite(userId: String, itemId: String): Result<Unit>
}