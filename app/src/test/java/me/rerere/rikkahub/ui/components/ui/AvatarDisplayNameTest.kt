package me.rerere.rikkahub.ui.components.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarDisplayNameTest {
    @Test
    fun `replaces the final extension with jpg`() {
        assertEquals("149654603_p0.jpg", avatarDisplayName("149654603_p0.png"))
        assertEquals("image.v2.jpg", avatarDisplayName("image.v2.webp"))
    }

    @Test
    fun `adds jpg when the source has no extension`() {
        assertEquals("123456_p0.jpg", avatarDisplayName("123456_p0"))
    }

    @Test
    fun `falls back when the source name is unavailable`() {
        assertEquals("avatar.jpg", avatarDisplayName(null))
        assertEquals("avatar.jpg", avatarDisplayName("  "))
        assertEquals("avatar.jpg", avatarDisplayName(".png"))
    }
}
