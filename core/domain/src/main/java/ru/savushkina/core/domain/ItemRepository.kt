package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.Item

data class Page<T>(
    val items: List<T>,
    val nextCursor: String?
)

interface ItemRepository {
    fun observeItems(limit: Long = 20): Flow<List<Item>>
    fun observeItemById(itemId: String): Flow<Item?>
    suspend fun getItemById(itemId: String): Item?
    suspend fun getItemsByIds(ids: List<String>): List<Item>

    suspend fun searchByName(query: String, limit: Long = 20): List<Item>

    suspend fun getTypes(): List<String>
    suspend fun getItemsByType(type: String, limit: Long = 50): List<Item>
    suspend fun getTopRated(limit: Long = 50): List<Item>
    suspend fun getLowestRated(limit: Long = 50): List<Item>

    /** Пагинация ленты. Возвращает Result, чтобы UI мог показать ошибку. */
    suspend fun loadItemsPage(limit: Long, lastDocId: String?): Result<Page<Item>>
}