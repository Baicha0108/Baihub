package me.rerere.rikkahub.data.files

import java.io.File
import java.net.URI
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.db.entity.MessageNodeEntity
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFileUrlRepairTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun filesDirWith(relative: String): Pair<File, File> {
        val filesDir = temp.newFolder("files")
        val file = File(filesDir, relative)
        file.parentFile?.mkdirs()
        file.writeText("test")
        return filesDir to file
    }

    private fun fileUrl(file: File): String =
        "file:///" + file.absolutePath.replace('\\', '/')

    @Test
    fun `rewrites old package path to current files dir`() {
        val (filesDir, target) = filesDirWith("upload/a.png")

        val repaired = repairFileUrl(
            "file:///data/user/0/me.rerere.rikkahub/files/upload/a.png",
            filesDir,
        )

        assertEquals(target.canonicalFile, File(URI.create(repaired)).canonicalFile)
    }

    @Test
    fun `rewrites data data form`() {
        val (filesDir, target) = filesDirWith("upload/b.png")

        val repaired = repairFileUrl(
            "file:///data/data/me.rerere.rikkahub/files/upload/b.png",
            filesDir,
        )

        assertEquals(target.canonicalFile, File(URI.create(repaired)).canonicalFile)
    }

    @Test
    fun `keeps readable file url unchanged`() {
        val (filesDir, target) = filesDirWith("upload/c.png")
        val url = fileUrl(target)

        assertEquals(url, repairFileUrl(url, filesDir))
    }

    @Test
    fun `ignores missing target`() {
        val filesDir = temp.newFolder("files")

        assertNull(
            repairFilePath(
                "/data/user/0/me.rerere.rikkahub/files/upload/missing.png",
                filesDir,
            )
        )
    }

    @Test
    fun `ignores unknown folders`() {
        val (filesDir, _) = filesDirWith("secret/a.png")

        assertNull(
            repairFilePath(
                "/data/user/0/me.rerere.rikkahub/files/secret/a.png",
                filesDir,
            )
        )
    }

    @Test
    fun `rejects path traversal`() {
        val filesDir = temp.newFolder("files")

        assertNull(
            repairFilePath(
                "/data/user/0/me.rerere.rikkahub/files/upload/../../secret",
                filesDir,
            )
        )
    }

    @Test
    fun `repairs assistant background and preset images`() {
        val filesDir = temp.newFolder("files")
        val background = File(filesDir, "upload/background.png").apply {
            parentFile?.mkdirs()
            writeText("background")
        }
        val preset = File(filesDir, "upload/preset.png").apply { writeText("preset") }
        val assistant = Assistant(
            background = "file:///data/user/0/me.rerere.rikkahub/files/upload/background.png",
            presetMessages = listOf(
                UIMessage(
                    role = MessageRole.ASSISTANT,
                    parts = listOf(
                        UIMessagePart.Image(
                            "file:///data/user/0/me.rerere.rikkahub/files/upload/preset.png"
                        )
                    ),
                )
            ),
        )

        val repaired = repairAssistantLocalFiles(assistant, filesDir)

        assertEquals(fileUrl(background), repaired.background)
        val image = repaired.presetMessages.single().parts.single() as UIMessagePart.Image
        assertEquals(fileUrl(preset), image.url)
    }

    @Test
    fun `rewrites export url to original package path`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")
        val image = File(source, "upload/a.png").apply {
            parentFile?.mkdirs()
            writeText("image")
        }

        val rewritten = rewriteFileUrl(fileUrl(image), source, target)

        assertEquals(
            File(target, "upload/a.png").canonicalPath,
            File(URI.create(rewritten)).canonicalPath,
        )
    }

    @Test
    fun `rewrites data data export url to original package path`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")
        val image = File(source, "upload/b.png").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        val oldUrl = "file:///data/data/me.baicha.baihub/files/upload/b.png"

        val rewritten = rewriteFileUrl(oldUrl, source, target)

        assertEquals(image.name, File(URI.create(rewritten)).name)
        assertEquals(
            File(target, "upload/b.png").canonicalPath,
            File(URI.create(rewritten)).canonicalPath,
        )
    }

    @Test
    fun `does not rewrite external file url`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")
        val url = "file:///storage/emulated/0/Download/photo.jpg"

        assertEquals(url, rewriteFileUrl(url, source, target))
    }

    @Test
    fun `does not rewrite path traversal`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")
        val url = "file:///data/user/0/me.baicha.baihub/files/upload/../../secret"

        assertEquals(url, rewriteFileUrl(url, source, target))
    }

    @Test
    fun `maps settings urls for export`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")

        fun sourceUrl(relative: String): String {
            val file = File(source, relative)
            file.parentFile?.mkdirs()
            file.writeText("image")
            return fileUrl(file)
        }

        val assistant = Assistant(
            avatar = Avatar.Image(sourceUrl("upload/avatar.png")),
            background = sourceUrl("upload/background.png"),
            presetMessages = listOf(
                UIMessage(
                    role = MessageRole.ASSISTANT,
                    parts = listOf(
                        UIMessagePart.Image(sourceUrl("upload/preset.png"))
                    ),
                )
            ),
        )
        val settings = Settings(
            assistants = listOf(assistant),
            displaySetting = DisplaySetting(
                userAvatar = Avatar.Image(sourceUrl("upload/user.png"))
            ),
        )

        val mapped = mapSettingsLocalFiles(settings) { url ->
            rewriteFileUrl(url, source, target)
        }

        fun assertMapped(url: String?, relative: String) {
            val value = requireNotNull(url)
            assertEquals(
                File(target, relative).canonicalPath,
                File(URI.create(value)).canonicalPath,
            )
        }

        val mappedAssistant = mapped.assistants.single()
        assertMapped((mappedAssistant.avatar as Avatar.Image).url, "upload/avatar.png")
        assertMapped(mappedAssistant.background, "upload/background.png")
        val presetImage = mappedAssistant.presetMessages.single().parts.single() as UIMessagePart.Image
        assertMapped(presetImage.url, "upload/preset.png")
        assertMapped(
            (mapped.displaySetting.userAvatar as Avatar.Image).url,
            "upload/user.png",
        )
    }

    @Test
    fun `rewrites message node urls for export`() {
        val source = File(temp.root, "me.baicha.baihub/files")
        val target = File(temp.root, "me.rerere.rikkahub/files")
        val image = File(source, "upload/message.png").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        val node = MessageNodeEntity(
            id = "node",
            conversationId = "conversation",
            nodeIndex = 0,
            messages = JsonInstant.encodeToString(
                listOf(
                    UIMessage(
                        role = MessageRole.USER,
                        parts = listOf(UIMessagePart.Image(fileUrl(image))),
                    )
                )
            ),
            selectIndex = 0,
        )

        val rewritten = requireNotNull(
            mapMessageNodeLocalFiles(node) { url ->
                rewriteFileUrl(url, source, target)
            }
        )

        val message = JsonInstant.decodeFromString<List<UIMessage>>(rewritten.messages).single()
        val part = message.parts.single() as UIMessagePart.Image
        assertEquals(
            File(target, "upload/message.png").canonicalPath,
            File(URI.create(part.url)).canonicalPath,
        )
    }
}
