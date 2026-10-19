package me.rerere.rikkahub.data.files

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.db.entity.MessageNodeEntity
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.mapLocalFileUrls
import me.rerere.rikkahub.utils.JsonInstant

private const val TAG = "LocalFileUrlRepair"

/**
 * Repairs local attachment URLs left behind by a backup restore into a different applicationId.
 *
 * Attachments are stored as absolute `file://` URLs pointing into the app's private
 * `filesDir`. Backups only contain relative attachment paths, so after restoring into an app
 * with a different package name the files exist in the new directory while the database still
 * points at the old one.
 */
class LocalFileUrlRepair(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
) {
    companion object {
        private const val PREFS_NAME = "baihub_local_file_url_repair"
        private const val KEY_VERSION = "version"
        private const val KEY_PACKAGE = "package"
        private const val VERSION = 2
    }

    data class Result(
        val repairedMessageRows: Int,
        val repairedAvatars: Int,
        val skipped: Boolean,
    )

    suspend fun repair(force: Boolean = false): Result = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val markerMatches = prefs.getInt(KEY_VERSION, 0) >= VERSION &&
            prefs.getString(KEY_PACKAGE, null) == context.packageName
        if (!force && markerMatches) {
            return@withContext Result(0, 0, skipped = true)
        }

        val filesDir = context.filesDir
        val repairedMessageRows = database.withTransaction {
            val dao = database.messageNodeDao()
            var changed = 0
            dao.getAllWithFileReferences().forEach { node ->
                val repaired = repairNode(node, filesDir) ?: return@forEach
                dao.update(repaired)
                changed++
            }
            changed
        }
        val repairedAvatars = repairSettings(filesDir)

        prefs.edit()
            .putInt(KEY_VERSION, VERSION)
            .putString(KEY_PACKAGE, context.packageName)
            .apply()

        if (repairedMessageRows > 0 || repairedAvatars > 0) {
            Log.i(
                TAG,
                "Repaired $repairedMessageRows message rows and $repairedAvatars avatars",
            )
        }
        Result(repairedMessageRows, repairedAvatars, skipped = false)
    }

    private fun repairNode(node: MessageNodeEntity, filesDir: File): MessageNodeEntity? =
        mapMessageNodeLocalFiles(node) { url -> repairFileUrl(url, filesDir) }

    private suspend fun repairSettings(filesDir: File): Int {
        val current = settingsStore.awaitLoaded()
        val repaired = mapSettingsLocalFiles(current) { url -> repairFileUrl(url, filesDir) }
        if (repaired == current) return 0

        settingsStore.update { latest ->
            mapSettingsLocalFiles(latest) { url -> repairFileUrl(url, filesDir) }
        }
        val changedAssistants = repaired.assistants.zip(current.assistants)
            .count { (after, before) -> after != before }
        val changedUserAvatar =
            repaired.displaySetting.userAvatar != current.displaySetting.userAvatar
        return changedAssistants + if (changedUserAvatar) 1 else 0
    }
}

internal val REPAIRABLE_FILE_FOLDERS = setOf(
    FileFolders.UPLOAD,
    FileFolders.SKILLS,
    FileFolders.BUILTIN_SKILLS,
    FileFolders.FONTS,
    FileFolders.TOOL_OUTPUTS,
    FileFolders.MEDIA_CREATION,
    "images",
)

/**
 * Returns a repaired URL, or the original URL when it is not a repairable local reference.
 * The target must already exist under [filesDir]; this never invents paths.
 */
internal fun repairFileUrl(url: String, filesDir: File): String {
    if (!url.startsWith("file://")) return url
    val path = runCatching { URI.create(url).path }.getOrNull() ?: return url
    val file = repairFilePath(path, filesDir) ?: return url
    return file.toFileUriString()
}

/**
 * Rewrites a local file URL under [sourceRoot] to the same relative path under [targetRoot].
 * The target does not need to exist yet; this is used while exporting a backup for another app.
 */
internal fun rewriteFileUrl(url: String, sourceRoot: File, targetRoot: File): String {
    if (!url.startsWith("file://")) return url
    val path = runCatching { URI.create(url).path }.getOrNull() ?: return url
    val relative = relativeExportPath(path, sourceRoot) ?: return url
    if (relative.isBlank()) return url
    if (relative.substringBefore('/') !in REPAIRABLE_FILE_FOLDERS) return url

    // Keep the user-visible path form (usually /data/user/0) instead of canonicalizing to
    // /data/data, which points at user 0 and breaks work profiles / secondary users.
    val root = targetRoot.absoluteFile.normalize()
    val target = File(root, relative).normalize()
    if (!target.path.startsWith(root.path + File.separator)) return url
    return target.toFileUriString()
}

/**
 * Extracts the attachment path from either the current filesDir form or another Android data
 * directory form (for example `/data/data/<package>/files/...`) of the same application.
 */
private fun relativeExportPath(path: String, sourceRoot: File): String? {
    val normalized = path.replace('\\', '/')
    val exactPrefix = sourceRoot.toURI().path.trimEnd('/') + "/"
    if (normalized.startsWith(exactPrefix)) {
        return normalized.removePrefix(exactPrefix)
    }
    if (!normalized.startsWith("/data/")) return null
    val packageName = sourceRoot.parentFile?.name ?: return null
    val marker = "/$packageName/files/"
    if (!normalized.contains(marker)) return null
    return normalized.substringAfter(marker)
}

internal fun mapMessageNodeLocalFiles(
    node: MessageNodeEntity,
    transform: (String) -> String,
): MessageNodeEntity? {
    val messages = runCatching {
        JsonInstant.decodeFromString<List<UIMessage>>(node.messages)
    }.getOrElse {
        Log.w(TAG, "Skipping unreadable message node ${node.id}", it)
        return null
    }
    val mapped = messages.map { message ->
        message.copy(
            parts = message.parts.map { part ->
                part.mapLocalFileUrls(transform)
            }
        )
    }
    if (mapped == messages) return null
    return node.copy(messages = JsonInstant.encodeToString(mapped))
}

internal fun mapAssistantLocalFiles(assistant: Assistant, transform: (String) -> String): Assistant =
    assistant.copy(
        avatar = mapAvatar(assistant.avatar, transform),
        background = assistant.background?.let(transform),
        presetMessages = assistant.presetMessages.map { message ->
            message.copy(
                parts = message.parts.map { part -> part.mapLocalFileUrls(transform) }
            )
        },
    )

internal fun mapSettingsLocalFiles(settings: Settings, transform: (String) -> String): Settings =
    settings.copy(
        assistants = settings.assistants.map { mapAssistantLocalFiles(it, transform) },
        displaySetting = settings.displaySetting.copy(
            userAvatar = mapAvatar(settings.displaySetting.userAvatar, transform),
        ),
    )

internal fun repairAssistantLocalFiles(assistant: Assistant, filesDir: File): Assistant =
    mapAssistantLocalFiles(assistant) { url -> repairFileUrl(url, filesDir) }

private fun mapAvatar(avatar: Avatar, transform: (String) -> String): Avatar =
    when (avatar) {
        is Avatar.Image -> avatar.copy(url = transform(avatar.url))
        else -> avatar
    }

/**
 * Maps an old absolute path such as
 * `/data/user/0/me.rerere.rikkahub/files/upload/a.png` to the same relative path under
 * [filesDir]. Returns null when the path is not a known attachment path or the target is absent.
 */
internal fun repairFilePath(path: String, filesDir: File): File? {
    if (path.isBlank()) return null
    val normalized = path.replace('\\', '/')
    val existing = File(normalized)
    if (existing.isFile && existing.canRead()) return null

    val marker = "/files/"
    val index = normalized.lastIndexOf(marker)
    if (index < 0) return null
    val relative = normalized.substring(index + marker.length)
    if (relative.isBlank()) return null
    if (relative.substringBefore('/') !in REPAIRABLE_FILE_FOLDERS) return null

    val root = filesDir.absoluteFile.normalize()
    val candidate = File(root, relative).normalize()
    if (!candidate.path.startsWith(root.path + File.separator)) return null
    if (!candidate.isFile || !candidate.canRead()) return null
    return candidate
}

private fun File.toFileUriString(): String {
    val uri = toURI().toString()
    return if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
        "file:///" + uri.removePrefix("file:/")
    } else {
        uri
    }
}
