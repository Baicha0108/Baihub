package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.model.backgroundImagesInUse
import me.rerere.rikkahub.data.model.normalizedBackgroundRotationIntervalSeconds

@Composable
fun AssistantBackground(setting: Settings, modifier: Modifier) {
    val assistant = setting.getCurrentAssistant()
    if (assistant.useGradientBackground) {
        MeshGradientBackground(modifier = modifier)
        return
    }
    val backgrounds = assistant.backgroundImagesInUse
    if (backgrounds.isNotEmpty()) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val backgroundOpacity = assistant.backgroundOpacity.coerceIn(0f, 1f)
        Box(modifier = modifier) {
            RotatingBackgroundImage(
                backgrounds = backgrounds,
                rotationEnabled = assistant.backgroundRotationEnabled,
                intervalSeconds = assistant.normalizedBackgroundRotationIntervalSeconds,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundOpacity)
            )

            if (setting.displaySetting.enableChatBackgroundScrim) {
                // 全屏渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColor.copy(alpha = 0.2f),
                                    backgroundColor.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun RotatingBackgroundImage(
    backgrounds: List<String>,
    rotationEnabled: Boolean,
    intervalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val shouldRotate = rotationEnabled && backgrounds.size > 1
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val initialIndex = remember(backgrounds, shouldRotate) {
        if (shouldRotate) {
            ((System.currentTimeMillis() / 1000L) % backgrounds.size).toInt()
        } else {
            0
        }
    }
    var index by remember(backgrounds, shouldRotate) {
        mutableIntStateOf(initialIndex)
    }

    LaunchedEffect(backgrounds, index, shouldRotate, intervalSeconds, lifecycleState) {
        if (!shouldRotate || lifecycleState != Lifecycle.State.RESUMED) {
            return@LaunchedEffect
        }
        delay(intervalSeconds.coerceAtLeast(1) * 1000L)
        index = (index + 1) % backgrounds.size
    }

    Crossfade(
        targetState = backgrounds[index],
        animationSpec = tween(durationMillis = 1000),
        modifier = modifier,
        label = "AssistantBackground",
    ) { background ->
        AsyncImage(
            model = background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
