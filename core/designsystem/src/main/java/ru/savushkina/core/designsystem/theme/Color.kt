package ru.savushkina.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ─── Базовая монохромная палитра ─────────────────────────────────
// Строго по шкале яркости, без цветовых примесей.

val Black = Color(0xFF000000)
val Gray05 = Color(0xFF0A0A0A)   // почти чёрный (фон поверхностей)
val Gray10 = Color(0xFF111111)
val Gray15 = Color(0xFF1A1A1A)   // карточки
val Gray20 = Color(0xFF222222)
val Gray30 = Color(0xFF2E2E2E)
val Gray40 = Color(0xFF3D3D3D)
val Gray50 = Color(0xFF555555)   // разделители
val Gray60 = Color(0xFF666666)
val Gray70 = Color(0xFF777777)   // неактивный текст
val Gray80 = Color(0xFF999999)   // второстепенный текст
val Gray90 = Color(0xFFCCCCCC)
val Gray95 = Color(0xFFE5E5E5)
val White = Color(0xFFFFFFFF)

// ─── Зелёный акцент ──────────────────────────────────────────────
// Три оттенка — на все случаи. Больше не нужно.

val GreenPrimary = Color(0xFF39FF14) // основной неон (кнопки, активные элементы)
val GreenMuted = Color(0xFF1F8C1F)   // приглушённый (вторичный акцент, иконки)
val GreenDim = Color(0xFF0F3D0F)     // очень тёмный (фон под акцентом)

// Полупрозрачные — для «свечения» и деликатных рамок.
val GreenPrimary12 = GreenPrimary.copy(alpha = 0.12f)
val GreenPrimary24 = GreenPrimary.copy(alpha = 0.24f)
val GreenPrimary60 = GreenPrimary.copy(alpha = 0.60f)