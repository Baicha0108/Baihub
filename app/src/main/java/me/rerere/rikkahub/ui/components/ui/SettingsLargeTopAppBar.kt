package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings large app bar with separate expanded and collapsed title layouts.
 *
 * Material3's LargeFlexibleTopAppBar passes the same title composable to both rows. A pill
 * background in that shared slot gets clipped while the expanded row collapses.
 */
@Composable
internal fun SettingsLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    TwoRowsTopAppBar(
        title = { expanded ->
            CompositionLocalProvider(
                LocalSettingsTopBarExpanded provides expanded,
                LocalTextStyle provides if (expanded) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
            ) {
                title()
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight = 64.dp,
        expandedHeight = 120.dp,
        windowInsets = TopAppBarDefaults.windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
