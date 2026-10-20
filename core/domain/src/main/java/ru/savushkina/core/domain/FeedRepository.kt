package ru.savushkina.core.domain

import kotlinx.coroutines.flow.Flow
import ru.savushkina.core.model.FeedItem

interface FeedRepository {
    /**
     * Лента активности пользователя. Один запрос, отсортировано по времени.
     * Пагинация — через [loadMore].
     */
    fun observeFeed(userId: String, limit: Long = DEFAULT_PAGE_SIZE): Flow<List<FeedItem>>

    /**
     * Догрузка следующей страницы. `lastTimestampMillis` — время последнего
     * элемента текущей страницы.
     */
    suspend fun loadMore(
        userId: String,
        lastTimestampMillis: Long,
        limit: Long = DEFAULT_PAGE_SIZE
    ): Result<List<FeedItem>>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50L
    }
}