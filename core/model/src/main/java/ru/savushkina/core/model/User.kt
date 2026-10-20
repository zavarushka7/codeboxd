package ru.savushkina.core.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val username: String = "",
    val description: String = "",
    val avatarBase64: String = "",
    val role: Role? = null,
    val onboardingCompleted: Boolean = false
) {
    val displayName: String
        get() = name.ifBlank {
            if (username.isNotBlank()) "@$username" else email.substringBefore('@')
        }

    val handle: String
        get() = if (username.isNotBlank()) "@$username" else ""

    val initials: String
        get() = displayName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
}