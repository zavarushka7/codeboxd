package ru.savushkina.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CodeboxShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

// Для «срезанных» углов (референс 3, голубая панель)
val ChamferSmall = RoundedCornerShape(0.dp, 6.dp, 0.dp, 6.dp)
val ChamferMedium = RoundedCornerShape(0.dp, 12.dp, 0.dp, 12.dp)