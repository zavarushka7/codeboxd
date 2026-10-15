
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
    val itemIconKey: String? = null
) {
    val hasComment: Boolean get() = comment.isNotBlank()
    val reviewId: String get() = "${userId}_$itemId"
}

/** Для отображения в списке — отзыв уже содержит всё, что нужно. */
typealias ReviewWithAuthor = Review
typealias ReviewWithItem = Review

