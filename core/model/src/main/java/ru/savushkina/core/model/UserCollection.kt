package ru.savushkina.core.model

import com.google.firebase.Timestamp

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
    val createdAt: Timestamp = Timestamp.now()
) {
    /** Можно ли добавлять/удалять айтемы. Только у списка. */
    val isEditable: Boolean
        get() = !isFixedSize

    /** Заполнен ли массив. */
    val isFull: Boolean
        get() = isFixedSize && itemIds.size >= maxSize

    companion object {
        const val MIN_SIZE = 2
        const val MAX_SIZE_LIMIT = 20
    }
}

enum class CollectionKind {
    LIST,
    ARRAY
}