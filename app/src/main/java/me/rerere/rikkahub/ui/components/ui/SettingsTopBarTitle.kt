package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A pill behind a settings app-bar title. The app bar applies its title fade to the whole
 * title slot, so this background follows the title's alpha automatically.
 */
internal val LocalSettingsTopBarExpanded = staticCompositionLocalOf { false }

@Composable
internal fun SettingsTopBarTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalSettingsTopBarExpanded.current) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                // Match the default 40dp diameter of the FilledTonalIconButton used by BackButton.
                .heightIn(min = 40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    shape = CircleShape,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}
