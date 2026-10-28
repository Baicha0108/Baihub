package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import me.rerere.bettertext.markdown.ListItem
import me.rerere.bettertext.markdown.MarkdownBlock
import me.rerere.bettertext.markdown.MarkdownDocument
import me.rerere.bettertext.markdown.MarkdownInline
import me.rerere.bettertext.markdown.plainText
import me.rerere.ui.text.EmojiTextRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiMarkdownTest {
    private val emojiFont = FontFamily.Serif

    @Test
    fun `styles emoji in nested inline content without changing plain text`() {
        val content = listOf(
            MarkdownInline.Text("普通 😀 内容"),
            MarkdownInline.Styled(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                children = listOf(MarkdownInline.Text("加粗 🚀")),
            ),
            MarkdownInline.Link(
                destination = "https://example.com",
                children = listOf(MarkdownInline.Text("链接 ❤️")),
            ),
            MarkdownInline.Code("代码 😀"),
        )
        val document = MarkdownDocument(
            source = "source",
            blocks = listOf(MarkdownBlock.Paragraph(content)),
        )

        val transformed = document.withEmojiFont(emojiFont)
        val paragraph = transformed.blocks.single() as MarkdownBlock.Paragraph

        assertEquals(content.plainText(), paragraph.content.plainText())
        assertEmojiIsStyledInText(paragraph.content)
        assertEquals(
            "代码 😀",
            (paragraph.content.last() as MarkdownInline.Code).code,
        )
    }

    @Test
    fun `styles emoji inside lists tables and details but not code blocks`() {
        val tableCell = MarkdownInline.Text("表格 😀")
        val document = MarkdownDocument(
            source = "source",
            blocks = listOf(
                MarkdownBlock.CodeBlock(code = "代码 🚀"),
                MarkdownBlock.ListBlock(
                    ordered = false,
                    items = listOf(
                        ListItem(
                            children = listOf(
                                MarkdownBlock.Paragraph(
                                    listOf(MarkdownInline.Text("列表 ❤️")),
                                ),
                            ),
                        ),
                    ),
                ),
                MarkdownBlock.Table(
                    header = listOf(
                        me.rerere.bettertext.markdown.TableCell(
                            listOf(MarkdownInline.Text("标题 😀")),
                        ),
                    ),
                    rows = listOf(
                        listOf(me.rerere.bettertext.markdown.TableCell(listOf(tableCell))),
                    ),
                ),
                MarkdownBlock.Details(
                    summary = listOf(MarkdownInline.Text("摘要 🚀")),
                    children = listOf(
                        MarkdownBlock.Paragraph(
                            listOf(MarkdownInline.Text("详情 ❤️")),
                        ),
                    ),
                ),
            ),
        )

        val transformed = document.withEmojiFont(emojiFont)

        assertEquals("代码 🚀", (transformed.blocks[0] as MarkdownBlock.CodeBlock).code)
        val list = transformed.blocks[1] as MarkdownBlock.ListBlock
        val listParagraph = list.items.single().children.single() as MarkdownBlock.Paragraph
        assertEmojiIsStyledInText(listParagraph.content)

        val table = transformed.blocks[2] as MarkdownBlock.Table
        assertEmojiIsStyledInText(table.header.single().content)
        assertEmojiIsStyledInText(table.rows.single().single().content)

        val details = transformed.blocks[3] as MarkdownBlock.Details
        assertEmojiIsStyledInText(details.summary)
        val detailsParagraph = details.children.single() as MarkdownBlock.Paragraph
        assertEmojiIsStyledInText(detailsParagraph.content)
    }

    private fun assertEmojiIsStyledInText(
        nodes: List<MarkdownInline>,
        inheritedEmojiFont: Boolean = false,
    ) {
        for (node in nodes) {
            when (node) {
                is MarkdownInline.Text -> {
                    if (EmojiTextRanges.findAll(node.text).isNotEmpty()) {
                        assertTrue("Emoji text was not styled: ${node.text}", inheritedEmojiFont)
                    }
                }
                is MarkdownInline.Styled -> assertEmojiIsStyledInText(
                    nodes = node.children,
                    inheritedEmojiFont = inheritedEmojiFont || node.style.fontFamily == emojiFont,
                )
                is MarkdownInline.Link -> assertEmojiIsStyledInText(
                    nodes = node.children,
                    inheritedEmojiFont = inheritedEmojiFont,
                )
                is MarkdownInline.Code,
                is MarkdownInline.Image,
                is MarkdownInline.Math,
                -> Unit
            }
        }
    }
}
