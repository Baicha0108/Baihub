package me.rerere.rikkahub.data.model

import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantBackgroundTest {
    @Test
    fun `legacy background is used when image list is absent`() {
        val assistant = Assistant(background = "file:///legacy.png")

        assertEquals(listOf("file:///legacy.png"), assistant.backgroundImagesInUse)
    }

    @Test
    fun `image list takes precedence over legacy background`() {
        val assistant = Assistant(
            background = "file:///legacy.png",
            backgroundImages = listOf("file:///one.png", "file:///two.png"),
        )

        assertEquals(
            listOf("file:///one.png", "file:///two.png"),
            assistant.backgroundImagesInUse,
        )
    }

    @Test
    fun `old assistant json decodes with single-image fallback`() {
        val assistant = JsonInstant.decodeFromString<Assistant>(
            """{"background":"file:///legacy.png"}"""
        )

        assertEquals(listOf("file:///legacy.png"), assistant.backgroundImagesInUse)
        assertTrue(!assistant.backgroundRotationEnabled)
        assertEquals(30, assistant.normalizedBackgroundRotationIntervalSeconds)
    }

    @Test
    fun `unsupported rotation interval falls back to thirty seconds`() {
        val assistant = Assistant(backgroundRotationIntervalSeconds = 7)

        assertEquals(30, assistant.normalizedBackgroundRotationIntervalSeconds)
    }

    @Test
    fun `rotation intervals match the configured slider positions`() {
        assertEquals(
            listOf(3, 5, 8, 10, 15, 30, 60, 300, 900, 1800),
            BACKGROUND_ROTATION_INTERVALS,
        )
    }

    @Test
    fun `multi-image rotation settings survive json round trip`() {
        val assistant = Assistant(
            background = "file:///one.png",
            backgroundImages = listOf("file:///one.png", "file:///two.png"),
            backgroundRotationEnabled = true,
            backgroundRotationIntervalSeconds = 900,
        )

        val decoded = JsonInstant.decodeFromString<Assistant>(
            JsonInstant.encodeToString(assistant)
        )

        assertEquals(assistant.backgroundImages, decoded.backgroundImages)
        assertTrue(decoded.backgroundRotationEnabled)
        assertEquals(900, decoded.backgroundRotationIntervalSeconds)
    }
}
