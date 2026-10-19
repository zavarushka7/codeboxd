package ru.savushkina.core.model

data class ReviewComment(
    val id: String = "",
    val reviewId: String = "",
    val authorId: String? = null,
    val authorName: String = "",
    val authorAvatarBase64: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L
)

enum class Reaction { LIKE, DISLIKE }