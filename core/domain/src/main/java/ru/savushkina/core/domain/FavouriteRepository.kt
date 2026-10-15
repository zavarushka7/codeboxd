package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow

interface FavouriteRepository {
    /** Наблюдает за списком избранного пользователя. */
    fun observeFavourites(userId: String): Flow<List<String>>

    /** Устанавливает полный список избранного (максимум 4). */
    suspend fun setFavourites(userId: String, itemIds: List<String>): Result<Unit>

    /** Добавляет айтем, если ещё не 4 и его нет в списке. */
    suspend fun addFavourite(userId: String, itemId: String): Result<Unit>

    /** Удаляет айтем из списка. */
    suspend fun removeFavourite(userId: String, itemId: String): Result<Unit>
}