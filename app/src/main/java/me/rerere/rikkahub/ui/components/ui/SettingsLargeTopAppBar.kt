package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * A transparent large settings app bar that does not clip its expanded title.
 *
 * The small title and large title are separate layers. The large title fades before it leaves
 * the app-bar area, so its upward motion never exposes a hard clip line.
 */
@Composable
internal fun SettingsLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    val density = LocalDensity.current
    val expandedHeight = 120.dp
    val collapsedHeight = 64.dp
    val expandedExtraPx = with(density) { (expandedHeight - collapsedHeight).toPx() }

    LaunchedEffect(scrollBehavior, expandedExtraPx) {
        scrollBehavior?.state?.heightOffsetLimit = -expandedExtraPx
    }

    val heightOffset = scrollBehavior?.state?.heightOffset ?: 0f
    val fraction = if (expandedExtraPx > 0f) {
        (-heightOffset / expandedExtraPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedHeight = expandedHeight + (collapsedHeight - expandedHeight) * fraction
    val containerColor = colors.containerColor
    val titleColor = colors.titleContentColor
    val navigationColor = colors.navigationIconContentColor
    val actionColor = colors.actionIconContentColor
    val smallTitleAlpha = fraction
    val largeTitleAlpha = (1f - fraction / 0.78f).coerceIn(0f, 1f)
    val largeTitleTranslation = with(density) { (-24.dp * fraction).toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .drawBehind { drawRect(containerColor) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(collapsedHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides navigationColor) {
                    navigationIcon()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .graphicsLayer { alpha = smallTitleAlpha },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalSettingsTopBarExpanded provides false,
                        LocalTextStyle provides MaterialTheme.typography.titleLarge,
                        LocalContentColor provides titleColor,
                    ) {
                        title()
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides actionColor) {
                        actions()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = largeTitleAlpha
                        translationY = largeTitleTranslation
                    },
                contentAlignment = Alignment.BottomStart,
            ) {
                CompositionLocalProvider(
                    LocalSettingsTopBarExpanded provides true,
                    LocalTextStyle provides MaterialTheme.typography.displaySmall,
                    LocalContentColor provides titleColor,
                ) {
                    title()
                }
            }
        }
    }
}
