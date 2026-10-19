package me.rerere.rikkahub.data.repository

import android.content.Context
import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.files.FileFolders
import me.rerere.rikkahub.data.model.SystemPromptEntry
import me.rerere.rikkahub.utils.JsonInstant
import kotlin.uuid.Uuid

/**
 * Per-assistant system prompt V2 entries, stored outside the settings JSON so the original
 * RikkaHub app never has to understand this fork-only structure.
 */
class SystemPromptV2Repository(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SystemPromptV2Repository"
    }

    private val cache = ConcurrentHashMap<Uuid, List<SystemPromptEntry>>()
    private val writeMutex = Mutex()
    private val directory: File
        get() = File(context.filesDir, FileFolders.SYSTEM_PROMPT_V2)

    private fun fileFor(assistantId: Uuid): File =
        File(directory, "$assistantId.json")

    suspend fun get(assistantId: Uuid): List<SystemPromptEntry> = withContext(Dispatchers.IO) {
        cache[assistantId]?.let { return@withContext it }
        val file = fileFor(assistantId)
        val entries = if (file.isFile) {
            runCatching {
                JsonInstant.decodeFromString<List<SystemPromptEntry>>(file.readText())
            }.getOrElse {
                Log.e(TAG, "Failed to read system prompt V2 for $assistantId", it)
                emptyList()
            }
        } else {
            emptyList()
        }
        cache[assistantId] = entries
        entries
    }

    suspend fun save(
        assistantId: Uuid,
        entries: List<SystemPromptEntry>,
    ) = withContext(Dispatchers.IO + NonCancellable) {
        writeMutex.withLock {
            cache[assistantId] = entries
            val target = fileFor(assistantId)
            if (entries.isEmpty()) {
                target.delete()
                return@withLock
            }

            if (!directory.isDirectory && !directory.mkdirs()) {
                error("Cannot create system prompt V2 directory: $directory")
            }
            val temporary = File(directory, "${assistantId}.json.tmp")
            temporary.writeText(JsonInstant.encodeToString(entries))
            runCatching {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    ATOMIC_MOVE,
                    REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(temporary.toPath(), target.toPath(), REPLACE_EXISTING)
            }
        }
    }

    suspend fun delete(assistantId: Uuid) = withContext(Dispatchers.IO + NonCancellable) {
        cache.remove(assistantId)
        fileFor(assistantId).delete()
    }

    suspend fun copy(fromAssistantId: Uuid, toAssistantId: Uuid) {
        val entries = get(fromAssistantId)
        if (entries.isNotEmpty()) {
            save(toAssistantId, entries.map { it.copy(id = Uuid.random()) })
        }
    }
}
