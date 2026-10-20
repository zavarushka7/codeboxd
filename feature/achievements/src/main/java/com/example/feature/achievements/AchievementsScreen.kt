package ru.savushkina.feature.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.savushkina.core.designsystem.component.AlertBox
import ru.savushkina.core.designsystem.component.BlinkingCursor
import ru.savushkina.core.designsystem.component.HorizontalLine
import ru.savushkina.core.designsystem.component.IconKey
import ru.savushkina.core.designsystem.component.LabelText
import ru.savushkina.core.designsystem.component.WireButton
import ru.savushkina.core.designsystem.component.WireFrame
import ru.savushkina.core.designsystem.pattern.TerminalScreen
import ru.savushkina.core.designsystem.pattern.TerminalTopBar
import ru.savushkina.core.designsystem.theme.Spacing
import ru.savushkina.core.model.Achievement
import ru.savushkina.core.model.AchievementLevels

@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalTopBar(
                title = "// награды",
                left = {
                    LabelText(
                        text = "< назад",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onBack)
                    )
                }
            )
            HorizontalLine()
            Spacer(Modifier.height(Spacing.md))

            when (val s = state) {
                is AchievementsUiState.Loading -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { BlinkingCursor() }
                is AchievementsUiState.Error -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        AlertBox(text = s.message)
                        WireButton(text = "повторить", onClick = viewModel::retry, accent = true)
                    }
                }
                is AchievementsUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.xxl),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(s.achievements, key = { it.type.name }) { achievement ->
                            AchievementCard(achievement = achievement)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    // Состояние «перевёрнута ли карточка» для конкретной награды.
    val flippedMap = remember { mutableStateMapOf<String, Boolean>() }
    val key = achievement.type.name
    val flipped = flippedMap[key] ?: false
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "achievement_rotation_$key"
    )

    // Если награда не получена — визуально гасим и показываем «заблокировано».
    val unlocked = achievement.isUnlocked

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flippedMap[key] = !flipped }
    ) {
        if (rotation <= 90f) {
            // Передняя сторона — иконка, название, уровень.
            WireFrame(
                modifier = Modifier.fillMaxSize(),
                accent = unlocked
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .alpha(if (unlocked) 1f else 0.35f),
                        contentAlignment = Alignment.Center
                    ) {
                        IconKey(
                            iconKey = achievement.iconKey,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = achievement.type.name.lowercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    LabelText(
                        text = if (unlocked) achievement.levelName else "заблокировано",
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    // Уровень: 1/5, 2/5, ... или 0/5.
                    LabelText(
                        text = "lvl ${achievement.level}/${AchievementLevels.MAX_LEVEL}",
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Задняя сторона — описание (уже перевёрнута обратно по оси Y).
            WireFrame(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                accent = unlocked
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = achievement.type.name.lowercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    LabelText(
                        text = if (achievement.nextThreshold != null)
                            "${achievement.currentValue} / ${achievement.nextThreshold}"
                        else
                            "макс: ${achievement.currentValue}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}