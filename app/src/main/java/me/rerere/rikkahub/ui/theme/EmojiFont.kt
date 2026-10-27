package me.rerere.rikkahub.ui.theme

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.files.loadEmojiFontFile
import me.rerere.ui.text.EmojiTextRanges

val LocalEmojiFontFamily = staticCompositionLocalOf<FontFamily?> { null }

@Composable
fun rememberEmojiFontFamily(displaySetting: DisplaySetting): FontFamily? {
    val context = LocalContext.current
    return remember(displaySetting.emojiFontPath) {
        loadEmojiFontFile(context, displaySetting.emojiFontPath)
            ?.let { FontFamily(Font(it)) }
    }
}

@Composable
fun rememberEmojiAnnotatedString(text: String): AnnotatedString {
    val emojiFontFamily = LocalEmojiFontFamily.current
    return remember(text, emojiFontFamily) {
        val annotatedText = AnnotatedString(text)
        if (emojiFontFamily == null) {
            annotatedText
        } else {
            annotatedText.withEmojiFont(emojiFontFamily)
        }
    }
}

@Composable
fun rememberEmojiAnnotatedString(text: AnnotatedString): AnnotatedString {
    val emojiFontFamily = LocalEmojiFontFamily.current
    return remember(text, emojiFontFamily) {
        if (emojiFontFamily == null) {
            text
        } else {
            text.withEmojiFont(emojiFontFamily)
        }
    }
}

@Composable
fun rememberEmojiVisualTransformation(
    delegate: VisualTransformation = VisualTransformation.None,
): VisualTransformation {
    val emojiFontFamily = LocalEmojiFontFamily.current
    return remember(delegate, emojiFontFamily) {
        emojiFontFamily?.let { EmojiVisualTransformation(delegate, it) } ?: delegate
    }
}

@Composable
fun rememberEmojiOutputTransformation(): OutputTransformation? {
    val emojiFontFamily = LocalEmojiFontFamily.current
    return remember(emojiFontFamily) {
        emojiFontFamily?.let { fontFamily ->
            OutputTransformation {
                EmojiTextRanges.findAll(asCharSequence()).forEach { range ->
                    addStyle(
                        spanStyle = SpanStyle(fontFamily = fontFamily),
                        start = range.first,
                        end = range.last + 1,
                    )
                }
            }
        }
    }
}

private class EmojiVisualTransformation(
    private val delegate: VisualTransformation,
    private val emojiFontFamily: FontFamily,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = delegate.filter(text)
        return TransformedText(
            text = transformed.text.withEmojiFont(emojiFontFamily),
            offsetMapping = transformed.offsetMapping,
        )
    }
}

private fun AnnotatedString.withEmojiFont(fontFamily: FontFamily): AnnotatedString {
    if (isEmpty()) return this

    return buildAnnotatedString {
        append(this@withEmojiFont)
        EmojiTextRanges.findAll(this@withEmojiFont).forEach { range ->
            addStyle(
                style = SpanStyle(fontFamily = fontFamily),
                start = range.first,
                end = range.last + 1,
            )
        }
    }
}
