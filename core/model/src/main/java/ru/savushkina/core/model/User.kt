package ru.savushkina.core.model

data class User (
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: Role? = null,
    val onboardingCompleted: Boolean = false
)