package me.rerere.rikkahub.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.files.loadEmojiFontFile

val LocalEmojiFontFamily = staticCompositionLocalOf<FontFamily?> { null }

@Composable
fun rememberEmojiFontFamily(displaySetting: DisplaySetting): FontFamily? {
    val context = LocalContext.current
    return remember(displaySetting.emojiFontPath) {
        loadEmojiFontFile(context, displaySetting.emojiFontPath)
            ?.let { FontFamily(Font(it)) }
    }
}
