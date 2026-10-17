package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.Item

interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    fun observeItemById(itemId: String): Flow<Item?>   // ← новый
    suspend fun getItemById(itemId: String): Item?
    suspend fun refreshItems(): Result<Unit>
    suspend fun searchByName(query: String): List<Item>
    suspend fun getTypes(): List<String>
    suspend fun getItemsByType(type: String): List<Item>
    suspend fun getTopRated(): List<Item>
    suspend fun getLowestRated(): List<Item>
    suspend fun searchByType(type: String): List<Item>
}