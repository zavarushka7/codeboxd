package ru.savushkina.core.model

data class UserCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val itemIds: List<String> = emptyList(),
    /** true — массив с фиксированным размером и типом. false — список. */
    val isFixedSize: Boolean = false,
    /** Размер. Используется только если isFixedSize = true. */
    val maxSize: Int = 0,
    /** Ограничение по типу айтема. Только для isFixedSize = true. */
    val allowedItemType: String? = null,
    /** millis since epoch */
    val createdAtMillis: Long = 0L
) {
    val isEditable: Boolean get() = !isFixedSize
    val isFull: Boolean get() = isFixedSize && itemIds.size >= maxSize

    companion object {
        const val MIN_SIZE = 1
        const val MAX_SIZE_LIMIT = 50
        const val NAME_MAX = 60
    }
}

enum class CollectionKind { LIST, ARRAY }