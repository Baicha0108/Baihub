package me.rerere.rikkahub.ui.pages.chat

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import kotlinx.coroutines.CancellationException

@Composable
internal fun PreloadNextBackground(
    backgrounds: List<String>,
    currentIndex: Int,
    currentImageLoaded: Boolean,
    enabled: Boolean,
    size: IntSize,
    lifecycleState: Lifecycle.State,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }

    LaunchedEffect(
        backgrounds,
        currentIndex,
        currentImageLoaded,
        enabled,
        size,
        lifecycleState,
        imageLoader,
    ) {
        if (
            !enabled ||
            !currentImageLoaded ||
            lifecycleState != Lifecycle.State.RESUMED ||
            backgrounds.size < 2 ||
            size.width <= 0 ||
            size.height <= 0
        ) {
            return@LaunchedEffect
        }

        val nextBackground = backgrounds[(currentIndex + 1) % backgrounds.size]
        val request = backgroundImageRequest(context, nextBackground, size)
        try {
            imageLoader.execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 预加载失败不阻塞当前背景，轮换时仍会按正常流程加载。
        }
    }
}

internal fun backgroundImageRequest(
    context: Context,
    background: String,
    size: IntSize,
): ImageRequest = ImageRequest.Builder(context)
    .data(background)
    .size(size.width, size.height)
    .scale(Scale.FILL)
    .precision(Precision.INEXACT)
    .build()
