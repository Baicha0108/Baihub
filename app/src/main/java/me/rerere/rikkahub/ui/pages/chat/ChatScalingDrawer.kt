package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private object ChatDrawerSpec {
    val WIDTH = 300.dp

    const val CONTENT_SCALE = 0.90f
    val CORNER_RADIUS = 22.dp
    val SHADOW = 20.dp
    const val SCRIM_ALPHA = 0.12f
    const val SETTLE_THRESHOLD = 0.25f
    val SettleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * 缩放式侧栏：聊天页右移并围绕左边缘缩放，露出侧栏和上下留白。
 * 只负责外层动画和手势，侧栏内容由调用方原样提供。
 * 交互参数参考 Yuihub（AGPL-3.0），按本项目侧栏宽度做了适配。
 */
@Composable
internal fun ChatScalingDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val backdropColor = DrawerDefaults.modalContainerColor
    val progress = remember { Animatable(if (open) 1f else 0f) }
    val progressState = remember(progress) { derivedStateOf { progress.value } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(open) {
        progress.animateTo(
            targetValue = if (open) 1f else 0f,
            animationSpec = ChatDrawerSpec.SettleSpec,
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val drawerWidth = ChatDrawerSpec.WIDTH.coerceAtMost(maxWidth)
        val dragSpanDp = maxWidth * 0.6f

        fun settle(startProgress: Float) {
            val delta = progress.value - startProgress
            val target = when {
                delta > ChatDrawerSpec.SETTLE_THRESHOLD -> true
                delta < -ChatDrawerSpec.SETTLE_THRESHOLD -> false
                else -> progress.value > 0.5f
            }
            val openChanged = target != open
            onOpenChange(target)
            if (!openChanged) {
                scope.launch {
                    progress.animateTo(
                        targetValue = if (target) 1f else 0f,
                        animationSpec = ChatDrawerSpec.SettleSpec,
                    )
                }
            }
        }

        fun dragBy(fraction: Float) {
            scope.launch {
                progress.snapTo((progress.value + fraction).coerceIn(0f, 1f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .drawBehind {
                    drawRect(
                        color = Color.Black,
                        alpha = ChatDrawerSpec.SCRIM_ALPHA * (1f - progressState.value),
                    )
                }
                .horizontalDrawerDrag(
                    gestureKey = open,
                    dragSpan = dragSpanDp,
                    progress = { progress.value },
                    onDrag = ::dragBy,
                    onSettle = ::settle,
                ),
        ) {
            DrawerSlidePanel(
                width = drawerWidth,
                progress = progressState,
                content = drawerContent,
            )

            ScaledHomePanel(
                drawerWidth = drawerWidth,
                progress = progressState,
                onTapWhenOpen = {
                    onOpenChange(false)
                },
                content = content,
            )
        }
    }
}

private fun Modifier.horizontalDrawerDrag(
    gestureKey: Any?,
    dragSpan: Dp,
    progress: () -> Float,
    onDrag: (Float) -> Unit,
    onSettle: (Float) -> Unit,
): Modifier = pointerInput(gestureKey, dragSpan) {
    val dragSpanPx = dragSpan.toPx()
    var startProgress = progress()
    detectHorizontalDragGestures(
        onDragStart = {
            startProgress = progress()
        },
        onHorizontalDrag = { _, dragAmount ->
            onDrag(dragAmount / dragSpanPx)
        },
        onDragEnd = { onSettle(startProgress) },
        onDragCancel = { onSettle(startProgress) },
    )
}

@Composable
private fun DrawerSlidePanel(
    width: Dp,
    progress: State<Float>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .graphicsLayer {
                translationX = -width.toPx() * (1f - progress.value)
            },
    ) {
        content()
    }
}

@Composable
private fun ScaledHomePanel(
    drawerWidth: Dp,
    progress: State<Float>,
    onTapWhenOpen: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnTapWhenOpen = rememberUpdatedState(onTapWhenOpen)
    val showTapBlocker = remember(progress) {
        derivedStateOf { progress.value > 0.01f }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val currentProgress = progress.value
                val scale = 1f - (1f - ChatDrawerSpec.CONTENT_SCALE) * currentProgress
                translationX = (drawerWidth * currentProgress).toPx()
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
                shadowElevation = (ChatDrawerSpec.SHADOW * currentProgress).toPx()
                shape = RoundedCornerShape(ChatDrawerSpec.CORNER_RADIUS * currentProgress)
                clip = true
            },
    ) {
        content()
        if (showTapBlocker.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { currentOnTapWhenOpen.value() }
                    },
            )
        }
    }
}
