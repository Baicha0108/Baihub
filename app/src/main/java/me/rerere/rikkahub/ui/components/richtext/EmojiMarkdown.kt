package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import me.rerere.bettertext.markdown.ListItem
import me.rerere.bettertext.markdown.MarkdownBlock
import me.rerere.bettertext.markdown.MarkdownDocument
import me.rerere.bettertext.markdown.MarkdownInline
import me.rerere.bettertext.markdown.TableCell
import me.rerere.ui.text.EmojiTextRanges

/**
 * Applies the selected Emoji font to parsed Markdown text while leaving regular text,
 * inline code and code blocks untouched.
 */
internal fun MarkdownDocument.withEmojiFont(fontFamily: FontFamily): MarkdownDocument {
    if (source.isEmpty()) return this
    return copy(blocks = blocks.map { it.withEmojiFont(fontFamily) })
}

private fun MarkdownBlock.withEmojiFont(fontFamily: FontFamily): MarkdownBlock = when (this) {
    is MarkdownBlock.Paragraph -> copy(content = content.withEmojiFont(fontFamily))
    is MarkdownBlock.Heading -> copy(content = content.withEmojiFont(fontFamily))
    is MarkdownBlock.Quote -> copy(children = children.map { it.withEmojiFont(fontFamily) })
    is MarkdownBlock.ListBlock -> copy(items = items.map { it.withEmojiFont(fontFamily) })
    is MarkdownBlock.Table -> copy(
        header = header.map { it.withEmojiFont(fontFamily) },
        rows = rows.map { row -> row.map { it.withEmojiFont(fontFamily) } },
    )
    is MarkdownBlock.Details -> copy(
        summary = summary.withEmojiFont(fontFamily),
        children = children.map { it.withEmojiFont(fontFamily) },
    )
    is MarkdownBlock.Container -> copy(children = children.map { it.withEmojiFont(fontFamily) })
    is MarkdownBlock.CodeBlock,
    MarkdownBlock.Divider,
    -> this
}

private fun ListItem.withEmojiFont(fontFamily: FontFamily): ListItem =
    copy(children = children.map { it.withEmojiFont(fontFamily) })

private fun TableCell.withEmojiFont(fontFamily: FontFamily): TableCell =
    copy(content = content.withEmojiFont(fontFamily))

private fun List<MarkdownInline>.withEmojiFont(fontFamily: FontFamily): List<MarkdownInline> =
    flatMap { node ->
        when (node) {
            is MarkdownInline.Text -> node.asEmojiNodes(fontFamily)
            is MarkdownInline.Styled -> listOf(
                node.copy(children = node.children.withEmojiFont(fontFamily)),
            )
            is MarkdownInline.Link -> listOf(
                node.copy(children = node.children.withEmojiFont(fontFamily)),
            )
            is MarkdownInline.Code,
            is MarkdownInline.Image,
            is MarkdownInline.Math,
            -> listOf(node)
        }
    }

private fun MarkdownInline.Text.asEmojiNodes(fontFamily: FontFamily): List<MarkdownInline> {
    val ranges = EmojiTextRanges.findAll(text)
    if (ranges.isEmpty()) return listOf(this)

    val result = mutableListOf<MarkdownInline>()
    var cursor = 0
    for (range in ranges) {
        if (cursor < range.first) {
            result += MarkdownInline.Text(text.substring(cursor, range.first))
        }
        result += MarkdownInline.Styled(
            style = SpanStyle(fontFamily = fontFamily),
            children = listOf(MarkdownInline.Text(text.substring(range.first, range.last + 1))),
        )
        cursor = range.last + 1
    }
    if (cursor < text.length) {
        result += MarkdownInline.Text(text.substring(cursor))
    }
    return result
}
