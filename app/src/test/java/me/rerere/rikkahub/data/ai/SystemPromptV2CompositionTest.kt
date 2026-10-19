package me.rerere.rikkahub.data.ai

import me.rerere.rikkahub.data.model.SystemPromptEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemPromptV2CompositionTest {
    @Test
    fun `appends enabled entries in order`() {
        val result = appendSystemPromptV2(
            base = "base",
            entries = listOf(
                SystemPromptEntry(title = "one", content = "first"),
                SystemPromptEntry(title = "two", content = "second"),
            ),
        )

        assertEquals("base\n\nfirst\n\n---\n\nsecond", result)
    }

    @Test
    fun `skips disabled and blank entries`() {
        val result = appendSystemPromptV2(
            base = "base",
            entries = listOf(
                SystemPromptEntry(title = "off", content = "ignored", enabled = false),
                SystemPromptEntry(title = "blank", content = "   "),
                SystemPromptEntry(title = "on", content = " kept "),
            ),
        )

        assertEquals("base\n\nkept", result)
    }

    @Test
    fun `returns base when no entries are enabled`() {
        val result = appendSystemPromptV2(
            base = "base",
            entries = listOf(
                SystemPromptEntry(title = "off", content = "ignored", enabled = false),
            ),
        )

        assertEquals("base", result)
    }

    @Test
    fun `entry becomes the prompt when base is blank`() {
        val result = appendSystemPromptV2(
            base = "",
            entries = listOf(SystemPromptEntry(title = "one", content = "only")),
        )

        assertEquals("only", result)
    }
}
