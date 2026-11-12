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
    fun `old assistant json decodes with new rotation defaults`() {
        val assistant = JsonInstant.decodeFromString<Assistant>(
            """{"background":"file:///legacy.png"}"""
        )

        assertEquals(listOf("file:///legacy.png"), assistant.backgroundImagesInUse)
        assertTrue(assistant.backgroundRotationEnabled)
        assertEquals(10, assistant.normalizedBackgroundRotationIntervalSeconds)
        assertEquals(1500, assistant.normalizedBackgroundRotationFadeMillis)
    }

    @Test
    fun `unsupported rotation interval falls back to ten seconds`() {
        val assistant = Assistant(backgroundRotationIntervalSeconds = 7)

        assertEquals(10, assistant.normalizedBackgroundRotationIntervalSeconds)
    }

    @Test
    fun `rotation intervals match the configured slider positions`() {
        assertEquals(
            listOf(3, 5, 8, 10, 15, 30, 60, 300, 900, 1800),
            BACKGROUND_ROTATION_INTERVALS,
        )
    }

    @Test
    fun `unsupported rotation fade falls back to fifteen hundred milliseconds`() {
        val assistant = Assistant(backgroundRotationFadeMillis = 250)

        assertEquals(1500, assistant.normalizedBackgroundRotationFadeMillis)
    }

    @Test
    fun `rotation fade durations match the configured slider positions`() {
        assertEquals(
            listOf(300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000),
            BACKGROUND_ROTATION_FADE_MILLIS,
        )
    }

    @Test
    fun `multi-image rotation settings survive json round trip`() {
        val assistant = Assistant(
            background = "file:///one.png",
            backgroundImages = listOf("file:///one.png", "file:///two.png"),
            backgroundRotationEnabled = true,
            backgroundRotationIntervalSeconds = 900,
            backgroundRotationFadeMillis = 1400,
        )

        val decoded = JsonInstant.decodeFromString<Assistant>(
            JsonInstant.encodeToString(assistant)
        )

        assertEquals(assistant.backgroundImages, decoded.backgroundImages)
        assertTrue(decoded.backgroundRotationEnabled)
        assertEquals(900, decoded.backgroundRotationIntervalSeconds)
        assertEquals(1400, decoded.backgroundRotationFadeMillis)
    }
}
