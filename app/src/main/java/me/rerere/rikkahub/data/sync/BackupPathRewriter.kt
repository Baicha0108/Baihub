package me.rerere.rikkahub.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import java.io.File
import me.rerere.rikkahub.data.db.AppDatabaseFactory
import me.rerere.rikkahub.data.files.mapMessageNodeLocalFiles
import me.rerere.rikkahub.data.files.rewriteFileUrl

internal const val ORIGINAL_APPLICATION_ID = "me.rerere.rikkahub"

/**
 * Makes backups exported by Baihub usable in the original RikkaHub app.
 *
 * The archive format is identical, but local attachments are stored as absolute `file://`
 * URLs. Rewriting the paths in the database snapshot and settings to the original
 * applicationId lets the original app restore the relative archive entries and find them.
 */
internal object BackupPathRewriter {
    private const val TAG = "BackupPathRewriter"

    fun originalFilesDir(context: Context): File =
        originalFilesDir(context.filesDir.absolutePath, context.packageName)

    suspend fun rewriteSnapshot(
        context: Context,
        snapshot: File,
        sourceFilesDir: File,
        targetFilesDir: File,
    ): Int {
        check(snapshot.isFile) { "Backup database snapshot is missing: $snapshot" }
        val database = AppDatabaseFactory.create(context, snapshot.absolutePath)
        var changed = 0
        try {
            database.withTransaction {
                val dao = database.messageNodeDao()
                dao.getAllWithFileReferences().forEach { node ->
                    val rewritten = mapMessageNodeLocalFiles(node) { url ->
                        rewriteFileUrl(url, sourceFilesDir, targetFilesDir)
                    } ?: return@forEach
                    dao.update(rewritten)
                    changed++
                }
            }
            DatabaseBackup.checkpoint(database.openHelper.writableDatabase)
        } finally {
            database.close()
        }
        DatabaseBackup.removeSidecars(snapshot)
        if (changed > 0) {
            Log.i(TAG, "Rewrote local file paths in $changed message rows")
        }
        return changed
    }
}

internal fun originalFilesDir(currentFilesDir: String, packageName: String): File {
    val marker = "/$packageName/"
    val index = currentFilesDir.lastIndexOf(marker)
    require(index >= 0) {
        "Unexpected files directory: $currentFilesDir"
    }
    val userDir = currentFilesDir.substring(0, index + 1)
    return File(userDir, "$ORIGINAL_APPLICATION_ID/files")
}
