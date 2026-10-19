package ru.savushkina.core.model

data class Review(
    val userId: String = "",
    val itemId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    // денормализация
    val authorName: String = "",
    val authorAvatarBase64: String = "",
    val itemName: String = "",
    val itemSymbol: String? = null,
    val itemIconKey: String? = null,
    val createdAtMillis: Long = 0L
) {
    val hasComment: Boolean get() = comment.isNotBlank()

    /**
     * Составной ID. `|` не может встретиться в Firestore ID (там запрещён только `/`),
     * но `|` мы явно запрещаем в uid/itemId при валидации на клиенте.
     * Если исторически уже есть данные с `_` — используем `|` как разделитель.
     */
    val reviewId: String get() = "$userId|$itemId"

    companion object {
        private const val SEP = '|'

        /** Возвращает (userId, itemId) или null, если формат некорректный. */
        fun parseReviewId(reviewId: String): Pair<String, String>? {
            val idx = reviewId.indexOf(SEP)
            if (idx <= 0 || idx == reviewId.length - 1) return null
            return reviewId.substring(0, idx) to reviewId.substring(idx + 1)
        }
    }
}

