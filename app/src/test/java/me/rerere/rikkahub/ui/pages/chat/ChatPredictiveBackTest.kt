package me.rerere.rikkahub.ui.pages.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatPredictiveBackTest {
    @Test
    fun `gesture start keeps the current progress`() {
        assertEquals(1f, predictiveDismissProgress(anchor = 1f, backProgress = 0f))
        assertEquals(0.6f, predictiveDismissProgress(anchor = 0.6f, backProgress = 0f))
    }

    @Test
    fun `gesture completion reaches closed`() {
        assertEquals(0f, predictiveDismissProgress(anchor = 1f, backProgress = 1f))
        assertEquals(0f, predictiveDismissProgress(anchor = 0.6f, backProgress = 1f))
    }

    @Test
    fun `progress tracks the back gesture from the sampled anchor`() {
        assertEquals(0.5f, predictiveDismissProgress(anchor = 1f, backProgress = 0.5f))
        assertEquals(0.3f, predictiveDismissProgress(anchor = 0.6f, backProgress = 0.5f))
    }

    @Test
    fun `out of range platform progress is clamped`() {
        assertEquals(1f, predictiveDismissProgress(anchor = 1f, backProgress = -0.2f))
        assertEquals(0f, predictiveDismissProgress(anchor = 1f, backProgress = 1.2f))
    }
}
