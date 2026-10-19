package me.rerere.rikkahub.data.export

import me.rerere.rikkahub.data.model.SystemPromptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPromptExportTest {
    @Test
    fun `export keeps entries separate and falls back to unnamed title`() {
        val export = buildCustomPromptExport(
            entries = listOf(
                SystemPromptEntry(title = "", content = "one", enabled = false),
                SystemPromptEntry(title = "two", content = "two"),
            ),
            unnamedTitle = "未命名",
        )

        assertEquals(1, export.version)
        assertEquals(2, export.entries.size)
        assertEquals("未命名", export.entries[0].title)
        assertEquals("one", export.entries[0].content)
        assertEquals(false, export.entries[0].enabled)
        assertEquals("two", export.entries[1].title)
    }

    @Test
    fun `same title replaces and different title appends`() {
        val existing = listOf(
            SystemPromptEntry(title = "角色设定", content = "old"),
            SystemPromptEntry(title = "回复风格", content = "style"),
        )
        val merge = mergeCustomPromptEntries(
            existing = existing,
            imported = listOf(
                CustomPromptExportEntry(title = "角色设定", content = "new"),
                CustomPromptExportEntry(title = "新规则", content = "rule"),
            ),
            unnamedTitle = "未命名",
        )

        assertEquals(listOf("角色设定"), merge.replacedTitles)
        assertEquals(listOf("新规则"), merge.appendedTitles)
        assertEquals(listOf("角色设定", "回复风格", "新规则"), merge.entries.map { it.title })
        assertEquals(listOf("new", "style", "rule"), merge.entries.map { it.content })
    }

    @Test
    fun `parse accepts object and bare array`() {
        val objectJson = """
            {
              "version": 1,
              "entries": [
                {"title": "a", "content": "b", "enabled": false}
              ]
            }
        """.trimIndent()
        val arrayJson = """
            [
              {"title": "c", "content": "d"}
            ]
        """.trimIndent()

        val objectExport = parseCustomPromptExport(objectJson)
        val arrayExport = parseCustomPromptExport(arrayJson)

        assertEquals("a", objectExport.entries.single().title)
        assertEquals(false, objectExport.entries.single().enabled)
        assertEquals("c", arrayExport.entries.single().title)
        assertTrue(arrayExport.entries.single().enabled)
    }
}
