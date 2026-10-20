package ru.savushkina.core.model

/**
 * Типы наград.
 *
 * Каждая награда имеет 5 уровней и пороги (см. [AchievementLevels]).
 * Уровень 0 — награда ещё не получена.
 */
enum class AchievementType {
    HATER,          // за отзывы с 1 звездой
    ENJOYER,        // за отзывы с 5 звёздами
    COUNT,          // за количество отзывов
    GRAPHOMANIAC,   // за длинные отзывы (> 200 символов)
    LIKES_RECEIVED, // полученные лайки за все отзывы
    LIKES_GIVEN,    // отданные лайки другим
    UNUSUAL         // ревью, чей рейтинг отличается от среднего на 2+
}

/**
 * Описание одной награды: тип, текущий уровень, текущее значение,
 * порог следующего уровня и тексты (имя/описание/иконка).
 */
data class Achievement(
    val type: AchievementType,
    val level: Int,                 // 0..5 (0 — не получена)
    val currentValue: Int,          // сколько уже набрано
    val nextThreshold: Int?,        // порог следующего уровня или null, если max
    val levelName: String,          // название текущего уровня (или "не получена")
    val description: String,        // длинное описание для обратной стороны
    val iconKey: String             // ключ иконки для IconKey
) {
    val isUnlocked: Boolean get() = level > 0
    val isMaxLevel: Boolean get() = level >= AchievementLevels.MAX_LEVEL
}

/**
 * Пороги и названия уровней для каждого типа награды.
 *
 * Индексация: level 1 → index 0, level 2 → index 1, ..., level 5 → index 4.
 */
object AchievementLevels {
    const val MAX_LEVEL = 5

    /** Пороги набранного значения для уровней 1..5. */
    val thresholds: Map<AchievementType, List<Int>> = mapOf(
        AchievementType.HATER to listOf(1, 3, 5, 10, 25),
        AchievementType.ENJOYER to listOf(1, 3, 5, 10, 25),
        AchievementType.COUNT to listOf(1, 5, 10, 25, 50),
        AchievementType.GRAPHOMANIAC to listOf(1, 3, 5, 10, 25),
        AchievementType.LIKES_RECEIVED to listOf(1, 10, 50, 100, 500),
        AchievementType.LIKES_GIVEN to listOf(1, 10, 50, 100, 500),
        AchievementType.UNUSUAL to listOf(1, 3, 5, 10, 15)
    )

    /** Названия уровней для каждого типа (5 штук на тип). */
    val levelNames: Map<AchievementType, List<String>> = mapOf(
        AchievementType.HATER to listOf(
            "начинающий критик", "скептик", "хейтер",
            "легендарный хейтер", "абсолютный хейтер"
        ),
        AchievementType.ENJOYER to listOf(
            "доброжелатель", "оптимист", "любитель",
            "энтузиаст", "абсолютный фанат"
        ),
        AchievementType.COUNT to listOf(
            "стажер", "джуниор", "джуниор+", "миддл", "сеньор"
        ),
        AchievementType.GRAPHOMANIAC to listOf(
            "рассказчик", "писатель", "графоман",
            "писатель-легенда", "мастер слова"
        ),
        AchievementType.LIKES_RECEIVED to listOf(
            "звездочка", "звезда", "любимец публики",
            "популярити", "легенда"
        ),
        AchievementType.LIKES_GIVEN to listOf(
            "поддерживающий", "доброжелатель", "щедрый",
            "меценат", "альтруист"
        ),
        AchievementType.UNUSUAL to listOf(
            "оригинал", "нонконформист", "бунтарь",
            "провокатор", "абсолютный бунтарь"
        )
    )

    /** Длинные описания для обратной стороны карточки. */
    val descriptions: Map<AchievementType, String> = mapOf(
        AchievementType.HATER to
                "За отзывы с оценкой 1 звезда. Чем больше таких отзывов — тем выше уровень.",
        AchievementType.ENJOYER to
                "За отзывы с оценкой 5 звёзд. Чем больше таких отзывов — тем выше уровень.",
        AchievementType.COUNT to
                "За общее количество оставленных отзывов.",
        AchievementType.GRAPHOMANIAC to
                "За длинные отзывы (больше 200 символов).",
        AchievementType.LIKES_RECEIVED to
                "За лайки, полученные на ваши отзывы от других пользователей.",
        AchievementType.LIKES_GIVEN to
                "За лайки, которые вы поставили другим отзывам.",
        AchievementType.UNUSUAL to
                "За отзывы, чей рейтинг отличается от вашего среднего на 2 и больше (при наличии минимум 2 отзывов)."
    )

    /** Иконки для каждой награды (ключи из IconKey.kt). */
    val icons: Map<AchievementType, String> = mapOf(
        AchievementType.HATER to "bug",
        AchievementType.ENJOYER to "palette",
        AchievementType.COUNT to "chart",
        AchievementType.GRAPHOMANIAC to "web",
        AchievementType.LIKES_RECEIVED to "security",
        AchievementType.LIKES_GIVEN to "build",
        AchievementType.UNUSUAL to "robot"
    )

    /** Вычисляет уровень по значению и типу. 0 — если не достигнут 1-й уровень. */
    fun levelFor(type: AchievementType, value: Int): Int {
        val thresholds = thresholds[type] ?: return 0
        var level = 0
        thresholds.forEachIndexed { index, threshold ->
            if (value >= threshold) level = index + 1
        }
        return level
    }

    /** Следующий порог после текущего уровня или null, если max. */
    fun nextThreshold(type: AchievementType, currentLevel: Int): Int? {
        val thresholds = thresholds[type] ?: return null
        if (currentLevel >= MAX_LEVEL) return null
        return thresholds[currentLevel]
    }

    /** Название текущего уровня (или "не получена", если level == 0). */
    fun levelName(type: AchievementType, currentLevel: Int): String {
        if (currentLevel <= 0) return "не получена"
        val names = levelNames[type] ?: return "не получена"
        return names.getOrNull(currentLevel - 1) ?: "не получена"
    }
}

/**
 * Снимок статистики пользователя, по которому считаются награды.
 * Заполняется в [UserRepository] / [ReviewRepository] через Firestore.
 */
data class AchievementStats(
    val oneStarReviews: Int = 0,
    val fiveStarReviews: Int = 0,
    val totalReviews: Int = 0,
    val longReviews: Int = 0,
    val likesReceived: Int = 0,
    val likesGiven: Int = 0,
    val unusualReviews: Int = 0
) {
    fun valueFor(type: AchievementType): Int = when (type) {
        AchievementType.HATER -> oneStarReviews
        AchievementType.ENJOYER -> fiveStarReviews
        AchievementType.COUNT -> totalReviews
        AchievementType.GRAPHOMANIAC -> longReviews
        AchievementType.LIKES_RECEIVED -> likesReceived
        AchievementType.LIKES_GIVEN -> likesGiven
        AchievementType.UNUSUAL -> unusualReviews
    }
}