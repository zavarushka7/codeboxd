package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.UserCollection

interface CollectionRepository {
    fun observeCollections(userId: String): Flow<List<UserCollection>>
    fun observeCollection(collectionId: String): Flow<UserCollection?>

    suspend fun createCollection(collection: UserCollection): Result<String>
    suspend fun addItem(collectionId: String, itemId: String): Result<Unit>
    suspend fun removeItem(collectionId: String, itemId: String): Result<Unit>
    suspend fun rename(collectionId: String, newName: String): Result<Unit>
    suspend fun deleteCollection(collectionId: String): Result<Unit>
}