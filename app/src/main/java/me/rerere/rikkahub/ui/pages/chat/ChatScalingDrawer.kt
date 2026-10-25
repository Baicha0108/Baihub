package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private object ChatDrawerSpec {
    val WIDTH = 300.dp

    const val CONTENT_SCALE = 0.90f
    val CORNER_RADIUS = 22.dp
    val SHADOW = 20.dp
    const val SCRIM_ALPHA = 0.12f
    const val SETTLE_THRESHOLD = 0.25f
    // 按钮、返回键和点击外部触发的开合从静止开始，使用平滑起步并准确落到 0/1。
    val ProgrammaticSpec = tween<Float>(
        durationMillis = 340,
        easing = FastOutSlowInEasing,
    )

    // 手指松开时仍有速度，缓动起点必须保留向前速度，避免“刹停后再启动”的停顿。
    val DragSettleSpec = tween<Float>(
        durationMillis = 300,
        easing = LinearOutSlowInEasing,
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
    val latestOnOpenChange = rememberUpdatedState(onOpenChange)

    LaunchedEffect(open) {
        val target = if (open) 1f else 0f
        if (
            (progress.targetValue == target && progress.isRunning) ||
            (!progress.isRunning && progress.value == target)
        ) {
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = target,
            animationSpec = ChatDrawerSpec.ProgrammaticSpec,
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
            // 先同步启动吸附动画，再更新 open 状态；LaunchedEffect 会识别到同一目标正在运行，
            // 从而避免状态切换时取消动画后重新开始。
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                progress.animateTo(
                    targetValue = if (target) 1f else 0f,
                    animationSpec = ChatDrawerSpec.DragSettleSpec,
                )
            }
            latestOnOpenChange.value(target)
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
                    dragSpan = dragSpanDp,
                    progress = { progress.value },
                    onDrag = { progress.snapTo(it) },
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
                blockClicks = open,
                onTapWhenOpen = {
                    onOpenChange(false)
                },
                content = content,
            )
        }
    }
}

private fun Modifier.horizontalDrawerDrag(
    dragSpan: Dp,
    progress: () -> Float,
    onDrag: suspend (Float) -> Unit,
    onSettle: (Float) -> Unit,
): Modifier = pointerInput(dragSpan) {
    val dragSpanPx = dragSpan.toPx()
    coroutineScope {
        // 拖动事件可能高于屏幕帧率。合并成最新值并由一个协程串行 snapTo，
        // 松手前会等待队列清空，避免迟到更新取消吸附动画。
        val dragUpdates = Channel<Float>(Channel.CONFLATED)
        val applyDragUpdates = launch {
            for (target in dragUpdates) {
                onDrag(target)
            }
        }
        var startProgress = progress()
        var dragTarget = startProgress
        var dragStarted = false

        detectHorizontalDragGestures(
            onDragStart = {
                startProgress = progress()
                dragTarget = startProgress
                dragStarted = true
            },
            onHorizontalDrag = { _, dragAmount ->
                dragTarget = (dragTarget + dragAmount / dragSpanPx).coerceIn(0f, 1f)
                dragUpdates.trySend(dragTarget)
            },
        )

        dragUpdates.close()
        applyDragUpdates.join()
        if (dragStarted) {
            onSettle(startProgress)
        }
    }
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
    blockClicks: Boolean,
    onTapWhenOpen: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnTapWhenOpen = rememberUpdatedState(onTapWhenOpen)
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
        if (blockClicks) {
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
