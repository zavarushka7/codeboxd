package ru.savushkina.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.savushkina.core.designsystem.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

// Базовые стили для терминального UI
private val monoRegular = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Normal
)
private val monoMedium = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Medium
)
private val monoBold = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold
)

val CodeboxTypography = Typography(
    // ── Крупные табло (24°C, 236,780,340, 12:01) ──
    displayLarge = monoBold.copy(
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1).sp
    ),
    displayMedium = monoBold.copy(
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp
    ),
    displaySmall = monoBold.copy(
        fontSize = 28.sp, lineHeight = 32.sp
    ),

    // ── Заголовки панелей (CAPS, широкий трекинг) ──
    headlineLarge = monoMedium.copy(
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 1.5.sp
    ),
    headlineMedium = monoMedium.copy(
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 1.5.sp
    ),
    headlineSmall = monoMedium.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.5.sp
    ),

    // ── Подзаголовки ──
    titleLarge = monoMedium.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.sp
    ),
    titleMedium = monoMedium.copy(
        fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 1.sp
    ),
    titleSmall = monoMedium.copy(
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.sp
    ),

    // ── Тело текста ──
    bodyLarge = monoRegular.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = monoRegular.copy(fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall = monoRegular.copy(fontSize = 11.sp, lineHeight = 16.sp),

    // ── Метки (LABEL, MIN, MAX, HUMIDITY) ──
    labelLarge = monoMedium.copy(
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 2.sp
    ),
    labelMedium = monoRegular.copy(
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 2.sp
    ),
    labelSmall = monoRegular.copy(
        fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 2.5.sp
    )
)