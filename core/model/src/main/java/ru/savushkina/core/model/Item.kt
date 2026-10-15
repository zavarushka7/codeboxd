package ru.savushkina.core.model

data class Item(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val symbol: String? = null,        // текстовый символ, если задан — показываем его вместо иконки
    val iconKey: String? = null,       // ключ иконки, если задан — показываем иконку
    val averageRating: Double = 0.0
) {
    val fallbackText: String
        get() = name.trim().take(2).uppercase().ifBlank { "?" }
}