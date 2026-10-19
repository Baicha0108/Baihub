package me.rerere.rikkahub.data.export

import kotlinx.serialization.Serializable
import me.rerere.rikkahub.data.model.SystemPromptEntry
import me.rerere.rikkahub.utils.JsonInstant

@Serializable
data class CustomPromptExport(
    val version: Int = 1,
    val entries: List<CustomPromptExportEntry> = emptyList(),
)

@Serializable
data class CustomPromptExportEntry(
    val title: String = "",
    val content: String = "",
    val enabled: Boolean = true,
)

data class CustomPromptMergeResult(
    val entries: List<SystemPromptEntry>,
    val replacedTitles: List<String>,
    val appendedTitles: List<String>,
)

fun buildCustomPromptExport(
    entries: List<SystemPromptEntry>,
    unnamedTitle: String,
): CustomPromptExport = CustomPromptExport(
    entries = entries.map {
        CustomPromptExportEntry(
            title = it.title.ifBlank { unnamedTitle },
            content = it.content,
            enabled = it.enabled,
        )
    }
)

/**
 * Entries with the same title replace the existing entry in place; new titles are appended
 * in file order. Returned title lists are used by the confirmation dialog.
 */
fun mergeCustomPromptEntries(
    existing: List<SystemPromptEntry>,
    imported: List<CustomPromptExportEntry>,
    unnamedTitle: String,
): CustomPromptMergeResult {
    val result = existing.toMutableList()
    val replacedTitles = mutableListOf<String>()
    val appendedTitles = mutableListOf<String>()

    imported.forEach { item ->
        val title = item.title.trim().ifBlank { unnamedTitle }
        val index = result.indexOfFirst { it.title.ifBlank { unnamedTitle } == title }
        if (index >= 0) {
            result[index] = result[index].copy(
                title = title,
                content = item.content,
                enabled = item.enabled,
            )
            replacedTitles.add(title)
        } else {
            result.add(
                SystemPromptEntry(
                    title = title,
                    content = item.content,
                    enabled = item.enabled,
                )
            )
            appendedTitles.add(title)
        }
    }

    return CustomPromptMergeResult(result, replacedTitles, appendedTitles)
}

fun parseCustomPromptExport(text: String): CustomPromptExport {
    val parsed = runCatching {
        JsonInstant.decodeFromString<CustomPromptExport>(text)
    }.getOrElse {
        // Also accept a bare array for files created by hand.
        CustomPromptExport(
            entries = JsonInstant.decodeFromString<List<CustomPromptExportEntry>>(text)
        )
    }
    require(parsed.version == 1) { "Unsupported custom prompt version: ${parsed.version}" }
    return parsed
}
