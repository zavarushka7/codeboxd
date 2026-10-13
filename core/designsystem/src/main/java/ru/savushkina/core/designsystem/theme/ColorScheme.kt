package ru.savushkina.core.designsystem.theme

import androidx.compose.material3.darkColorScheme

val CodeboxColorScheme = darkColorScheme(
    // ── Основной акцент ──
    primary = GreenPrimary,
    onPrimary = Black,
    primaryContainer = GreenDim,
    onPrimaryContainer = GreenPrimary,

    // ── Вторичный — приглушённый зелёный ──
    secondary = GreenMuted,
    onSecondary = Black,
    secondaryContainer = GreenDim,
    onSecondaryContainer = GreenPrimary,

    // ── Третичный — белый (для «чистых» акцентов) ──
    tertiary = White,
    onTertiary = Black,

    // ── Фон ──
    background = Black,
    onBackground = Gray95,

    // ── Поверхности ──
    surface = Gray05,
    onSurface = Gray95,
    surfaceVariant = Gray15,
    onSurfaceVariant = Gray80,
    surfaceContainer = Gray10,
    surfaceContainerHigh = Gray15,
    surfaceContainerHighest = Gray20,

    // ── Границы ──
    outline = GreenMuted,
    outlineVariant = Gray40,

    // ── Ошибка — приглушённый зелёный ──
    // (семантически остаётся, но визуально не выбивается из палитры)
    error = GreenMuted,
    onError = Black,
    errorContainer = GreenDim,
    onErrorContainer = GreenPrimary,

    // ── Служебные ──
    scrim = Black.copy(alpha = 0.8f),
    inverseSurface = Gray95,
    inverseOnSurface = Black,
    inversePrimary = GreenDim
)