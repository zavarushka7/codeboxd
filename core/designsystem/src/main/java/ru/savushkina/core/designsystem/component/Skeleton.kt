package ru.savushkina.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.Sizes
import ru.savushkina.core.designsystem.theme.Spacing

/**
 * Базовый skeleton-блок с пульсацией.
 *
 * ВАЖНО (про `width`):
 * - `width == null`  → Skeleton растягивается на всю ширину родителя.
 * - `width != null`  → Skeleton ровно `width` шириной (не больше и не меньше).
 *
 * Раньше использовалось `fillMaxWidth().widthIn(max = width)`, но это НЕ работало:
 * `fillMaxWidth` задаёт `minWidth = maxWidth = parentWidth`, а `widthIn(max = width)`
 * пытается сузить `maxWidth` при уже установленном `minWidth = parentWidth`.
 * В Compose при конфликте `minWidth > maxWidth` ограничение игнорируется —
 * Skeleton всегда был во всю ширину. Заменили на `Modifier.width(width)`.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    cornerRadius: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    val sizeModifier: Modifier = if (width != null) {
        Modifier.width(width)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/**
 * Заглушка строки айтема — повторяет ItemRow.
 */
@Composable
fun SkeletonItemRow(modifier: Modifier = Modifier) {
    WireFrame(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Skeleton(
                modifier = Modifier.size(Sizes.iconFrame),
                height = Sizes.iconFrame,
                cornerRadius = 0.dp
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Skeleton(height = 16.dp)
                Spacer(Modifier.height(Spacing.xs))
                Skeleton(height = 12.dp, width = 80.dp)
            }
        }
    }
}

/**
 * Заглушка строки отзыва — повторяет ReviewRow.
 */
@Composable
fun SkeletonReviewRow(modifier: Modifier = Modifier) {
    WireFrame(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Skeleton(
                    modifier = Modifier.size(32.dp),
                    height = 32.dp,
                    cornerRadius = 0.dp
                )
                Spacer(Modifier.width(Spacing.sm))
                Skeleton(height = 14.dp, width = 100.dp)
            }
            Spacer(Modifier.height(Spacing.sm))
            Skeleton(height = 12.dp)
            Spacer(Modifier.height(Spacing.xs))
            Skeleton(height = 12.dp, width = 240.dp)
        }
    }
}

/**
 * Заглушка заголовка профиля.
 */
@Composable
fun SkeletonProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Skeleton(
            modifier = Modifier.size(120.dp),
            height = 120.dp,
            cornerRadius = 0.dp
        )
        Spacer(Modifier.height(Spacing.lg))
        Skeleton(height = 22.dp, width = 180.dp)
        Spacer(Modifier.height(Spacing.md))
        Skeleton(height = 14.dp, width = 120.dp)
    }
}

/**
 * Список skeleton-строк.
 *
 * НЕ добавляет горизонтальный padding — это ответственность вызывающего
 * экрана. Иначе на ленте skeleton уже, чем реальные карточки (ItemRow).
 */
@Composable
fun SkeletonList(
    count: Int = 6,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        repeat(count) {
            content()
        }
    }
}