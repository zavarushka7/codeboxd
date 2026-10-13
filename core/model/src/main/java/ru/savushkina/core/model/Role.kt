package ru.savushkina.core.model

enum class Role(val displayName: String) {
    DEVELOPMENT("Разработка"),
    ANALYTICS("Аналитика"),
    CYBERSECURITY("Кибербезопасность"),
    DEVOPS("DevOps"),
    DATA_SCIENCE("Data Science"),
    MACHINE_LEARNING("Машинное обучение"),
    DO_NOT_SPECIFY("Не указывать")
}