package me.rerere.rikkahub.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPathRewriterTest {
    @Test
    fun `maps user zero path to original package`() {
        val dir = originalFilesDir(
            "/data/user/0/me.baicha.baihub/files",
            "me.baicha.baihub",
        )

        assertEquals(
            "/data/user/0/me.rerere.rikkahub/files",
            dir.path.replace('\\', '/'),
        )
    }

    @Test
    fun `maps data data path to original package`() {
        val dir = originalFilesDir(
            "/data/data/me.baicha.baihub/files",
            "me.baicha.baihub",
        )

        assertEquals(
            "/data/data/me.rerere.rikkahub/files",
            dir.path.replace('\\', '/'),
        )
    }

    @Test
    fun `maps debug package path`() {
        val dir = originalFilesDir(
            "/data/user/0/me.baicha.baihub.debug/files",
            "me.baicha.baihub.debug",
        )

        assertEquals(
            "/data/user/0/me.rerere.rikkahub/files",
            dir.path.replace('\\', '/'),
        )
    }
}
