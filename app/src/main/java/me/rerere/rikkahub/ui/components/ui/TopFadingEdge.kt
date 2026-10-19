package me.rerere.rikkahub.ui.components.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import me.rerere.rikkahub.ui.theme.CustomColors

// 顶部渐隐：内容滚到标题栏下面时逐渐透明，从状态栏一直淡出到标题栏下沿之外，保证标题可读。
// 让内容自己变透明而不是盖一层背景色，背景图和动态渐变背景才不会被盖住。
internal fun Modifier.topFadingEdge(height: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val fadeHeight = height.toPx()
        // Use a sampled quartic curve instead of a few hard stops. A stop such as
        // "60% at 15% alpha, then 100% at 100%" creates a visible slope discontinuity
        // (a horizontal band) across the fade.
        val steps = 64
        val colors = List(steps + 1) { index ->
            val t = index / steps.toFloat()
            Color.Black.copy(alpha = t * t * t * t)
        }
        val brush = Brush.verticalGradient(
            colors = colors,
            startY = 0f,
            endY = fadeHeight,
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush, size = Size(size.width, fadeHeight), blendMode = BlendMode.DstIn)
        }
    }

/**
 * A soft scrim over the content instead of fading it to transparent. Starts at [maxAlpha]
 * at the top and reaches zero at [height], so card backgrounds are gently tinted towards
 * [color] without a hard alpha cutoff.
 */
internal fun Modifier.topScrimEdge(
    height: Dp,
    color: Color,
    maxAlpha: Float = 0.7f,
): Modifier = drawWithCache {
    val fadeHeight = height.toPx()
    val steps = 64
    val colors = List(steps + 1) { index ->
        val t = index / steps.toFloat()
        val alpha = maxAlpha * (1f - t) * (1f - t)
        color.copy(alpha = alpha)
    }
    val brush = Brush.verticalGradient(
        colors = colors,
        startY = 0f,
        endY = fadeHeight,
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush, size = Size(size.width, fadeHeight))
    }
}

@Composable
internal fun Modifier.settingsTopScrim(topPadding: Dp): Modifier {
    val color = CustomColors.topBarColors.containerColor
    return topScrimEdge(
        height = (topPadding + 24.dp) / 2,
        color = color,
        maxAlpha = 0.7f,
    )
}
